# admin 모듈

Spring Boot 앱 (8090포트, `admin-api.apiverse.com`). `@SpringBootApplication(scanBasePackages = "com.hypepia.apiverse")`로 core 포함 스캔. gateway와 core를 공유하며, gateway가 8080에서 하던 관리자 전용 기능(상품 승인, 문의 답변 등)을 별도 앱으로 분리하는 작업 중.

## 패키지 구조

```
admin/
├── config/     SecurityConfig, JwtUtils, JwtWebFilter (gateway와 동일 패턴)
├── auth/       AdminAuthController (로그인만, 회원가입 없음)
├── product/    ProductAdminController, UpdateProductRequest
├── inquiry/    InquiryAdminController, AnswerRequest
├── user/       UserAdminController, UpdateTierRequest
├── key/        ApiKeyAdminController
├── usage/      UsageAdminController
├── blockedip/  BlockedIpAdminController, AddBlockedIpRequest
└── stats/      StatsAdminController
```

요청 DTO(`XxxRequest`)는 gateway와 동일하게 해당 기능 패키지에 위치.

## 엔드포인트

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/admin/auth/login` | role이 ADMIN 아니면 403 |
| GET | `/api/admin/products` | 전체 상품 목록 (code/upstreamApiKey/upstreamKeyParam 포함) |
| GET | `/api/admin/products/pending` | 승인 대기 상품 목록 |
| GET | `/api/admin/products/{id}` | 상품 상세 |
| PATCH | `/api/admin/products/{id}/approve` | 상품 승인 (isActive=true) |
| DELETE | `/api/admin/products/{id}/reject` | 승인 대기 상품 반려(삭제). 이미 승인된 상품은 409 |
| PATCH | `/api/admin/products/{id}` | 상품 수정 (description/baseUrl/category/callsPerSec/responseType/isPremium/specJson/code/upstreamApiKey/upstreamKeyParam, null 필드는 무시) |
| GET | `/api/admin/inquiries` | 전체 문의 목록 |
| GET | `/api/admin/inquiries/{id}` | 문의 상세 |
| POST | `/api/admin/inquiries/{id}/answer` | 답변 등록 (status→ANSWERED) |
| GET | `/api/admin/users` | 전체 회원 목록 (password_hash 제외) |
| GET | `/api/admin/users/{id}` | 회원 상세 |
| PATCH | `/api/admin/users/{id}/tier` | 회원 tier 변경 |
| GET | `/api/admin/keys` | 전체 API 키 목록 (userEmail/apiProductName/whiteListIp 포함) |
| GET | `/api/admin/keys/{id}` | API 키 상세 |
| DELETE | `/api/admin/keys/{id}` | API 키 강제 폐기 (소유자 무관) |
| PATCH | `/api/admin/keys/{id}/whitelist-ip` | 허용 IP 설정 (콤마로 여러 개, 빈 값이면 제한 해제) |
| PATCH | `/api/admin/keys/{id}/quota` | 월간 쿼터 설정 (`monthlyQuota`: -1=무제한 또는 0 이상, 그 외 값은 400) |
| GET | `/api/admin/usage/daily` | 전체 유저 대상 7일 일별 요청/에러 통계 |
| GET | `/api/admin/usage/logs` | `billing_logs` 원본 목록, `onlyErrors`(5xx만)/`days`(기본 7, 최대 90)/`page`/`size`(최대 200) 페이지네이션 |
| GET | `/api/admin/blocked-ips` | 전역 차단 IP 목록 |
| POST | `/api/admin/blocked-ips` | 차단 IP 추가 (`ipAddress`, `reason` 선택), 중복 시 409 |
| DELETE | `/api/admin/blocked-ips/{id}` | 차단 해제 |
| GET | `/api/admin/stats/products-errors` | 상품별 에러율 랭킹 — 호출량 대비 5xx 비율 내림차순 (`days` 기본 7, 최대 90) |
| GET | `/api/admin/stats/top-error-keys` | 에러 많은 API 키 Top N — 에러 건수 기준 (`days`, `limit` 기본 20 최대 100) |
| GET | `/api/admin/stats/status-codes` | 상태코드 분포 (`days`) |
| GET | `/api/admin/stats/products-usage` | 상품별 사용량 랭킹 — 호출량 기준 (`days`) |
| GET | `/api/admin/stats/top-usage-keys` | 호출량 많은 API 키 Top N — 에러 유무 무관 (`days`, `limit`) |
| GET | `/api/admin/stats/quota-usage` | 월 쿼터 사용률 Top N, `monthlyQuota=-1`(무제한) 제외 (`limit`) |

`/api/admin/auth/login`만 `permitAll`, 나머지는 `authenticated()`.

## 전역 IP 차단

`blocked_ips` 테이블에 등록된 IP는 API 키/상품 승인 여부와 무관하게 `/gateway/**` 요청 자체가 `ProxyService.proxy()` 최상단(API 키 검증보다 먼저)에서 403으로 거부된다 (`gateway.md`의 "전역 IP 차단" 참고). 회원별 API 키 화이트리스트(`api_keys.white_list_ip`, "허용" 목록)와는 반대 개념(전역 "차단" 목록)이며 서로 독립적으로 동작한다.

## 통계 (StatsAdminController)

에러 대응(트리아지)용 3종 + 사용률(용량 계획)용 3종, 총 6개 통계. `days`는 1~90 사이로 clamp.

**에러 대응용:**
- **상품별 에러율 랭킹**(`products-errors`): `billing_logs` ⟕ `api_keys` ⟕ `api_products` 조인 후 상품별 전체 요청/5xx 건수 집계, `error_count::float / total_requests` 내림차순 — 호출량이 적어도 실패율이 높으면 상위에 노출되어야 하므로 건수가 아닌 비율로 정렬한다.
- **에러 많은 API 키 Top N**(`top-error-keys`): 위 조인에 `users`까지 더해 API 키/회원 단위로 집계, 에러 건수 기준 정렬, 에러가 0건 초과인 것만(`HAVING`) 반환. 특정 연동사의 통합 문제인지 식별.
- **상태코드 분포**(`status-codes`): `response_status`별 건수. 4xx(클라이언트 오류)와 5xx(서버/업스트림 오류) 비중 구분용.

**사용률(용량 계획)용:**
- **상품별 사용량 랭킹**(`products-usage`): `products-errors`와 동일 집계, 정렬만 `total_requests` 기준 — 어떤 상품이 가장 많이 호출되는지.
- **호출량 많은 API 키 Top N**(`top-usage-keys`): `top-error-keys`와 동일 집계, `HAVING` 없이 전체 대상 + `total_requests` 기준 정렬.
- **월 쿼터 사용률 Top N**(`quota-usage`): `billing_logs` 조인 없이 `api_keys` ⟕ `users` ⟕ `api_products`만으로 `used_quota / monthly_quota` 계산, `monthly_quota = -1`(무제한) 제외. 쿼터 소진 임박 회원을 미리 파악하는 용도라 `days` 파라미터가 없다(현재 시점 스냅샷).

쿼리는 `BillingLogRepository`(5종)와 `ApiKeyRepository`(`quota-usage` 1종)에 나뉘어 있으며, 기간 조건은 `NOW() - (INTERVAL '1 day' * :days)`로 파라미터화한다(문자열 결합 대신 interval 곱셈 사용 — R2DBC 바인딩 파라미터로 안전하게 처리).

## role vs tier

`users` 테이블은 두 컬럼을 분리해서 쓴다:
- `tier` — 과금 등급 (`FREE`/`PRO`/`ENTERPRISE`). 서비스 요금제와만 관련.
- `role` — 권한 (`USER`/`ADMIN`). 관리자 여부만 나타냄.

원래는 `tier`에 `ADMIN` 값을 끼워 넣어 사용했으나, 이 경우 회원 tier 변경 API(`PATCH /api/admin/users/{id}/tier`) 하나로 아무 계정이나 관리자로 승격시킬 수 있는 권한 상승 경로가 생겨 `role` 컬럼으로 분리했다. `role`을 바꾸는 API는 의도적으로 만들지 않았다 — 관리자 계정은 DB에서 직접 생성한다 (`gateway/src/main/resources/data.sql`의 `admin@apiverse.com` 시드 참고).

## 상품 code / 업스트림 키

`ApiProduct`의 `upstreamApiKey`/`upstreamKeyParam` 필드는 `@JsonIgnore`가 붙어 있어 엔티티를 그대로 직렬화하면 어떤 컨트롤러에서도 값이 나오지 않는다 (gateway의 공개 `/api/products` 등에서 실수로 노출되는 것을 막기 위한 방어). 그래서 `ProductAdminController`는 다른 admin 컨트롤러들과 달리 `ApiProduct`를 직접 반환하지 않고, `toProductMap()`으로 필요한 필드(코드/업스트림 키 포함)를 명시적으로 담은 `Map`을 반환한다 — 이 패턴을 벗어나 엔티티를 직접 리턴하는 상품 관련 엔드포인트를 새로 추가하면 업스트림 키가 조용히 안 보이게 되므로 주의.

`code`는 `/gateway/{code}/**` 라우팅에 쓰이는 슬러그로, `gateway.md`의 "code 생성" 참고. `upstreamKeyParam`은 `header:{헤더명}` 또는 `query:{파라미터명}` 형식.

## 인증 및 권한 재검증

- `JwtWebFilter`는 토큰 유효성만 검증하고 role은 확인하지 않는다 — 로그인 시점에만 `AdminAuthController`가 role을 확인.
- 따라서 **모든 컨트롤러가 매 요청마다 자체적으로 ADMIN role을 재검증**해야 한다. 각 컨트롤러에 `requireAdmin(Long uid)` private 헬퍼를 두고 `UserRepository.findById`로 조회 후 role 비교 (gateway의 `InquiryController.answer()`와 동일한 인라인 패턴). 새로운 admin 컨트롤러를 추가할 때 이 체크를 빠뜨리면 로그인한 일반 유저도 접근 가능해지므로 반드시 포함할 것.

## core에 admin 전용으로 추가된 레포지토리 메서드

- `UserRepository.findAllByOrderByIdDesc()`
- `ApiKeyRepository.findAllByOrderByIdDesc()`
- `ApiProductRepository.findAllByOrderByIdDesc()`, `findByCode(String code)` (gateway 프록시가 code로 상품 조회할 때 사용)
- `InquiryRepository.findAllByOrderByCreatedAtDesc()`
- `BillingLogRepository.findDailyStatsGlobal()` — `findDailyStatsByUserId`와 동일 쿼리에서 `user_id` 필터만 제거
- `BillingLogRepository.findLogsPage(onlyErrors, days, size, offset)` / `countLogs(onlyErrors, days)` — `/api/admin/usage/logs` 페이지네이션용. `onlyErrors=true`면 `response_status >= 500`(usage/daily의 errors와 동일 기준)만 반환. `days`로 조회 기간 상한(컨트롤러에서 1~90 clamp) — 무제한 전체 테이블 스캔 방지

## 빌드 관련 주의사항

- `admin`은 `core`를 `implementation project(':core')`로 의존하지만, `core`가 `spring-boot-starter-data-r2dbc`를 `implementation`으로 선언하고 있어 **전이(transitive)되지 않는다**. `admin/build.gradle`에 `spring-boot-starter-data-r2dbc` + `r2dbc-postgresql`을 직접 선언해야 `ReactiveCrudRepository` 등이 컴파일된다. gateway는 `spring-boot-starter-data-redis-reactive`를 직접 선언하고 있어 그 전이 의존성으로 우연히 spring-data-commons를 받고 있었을 뿐, 같은 문제를 안고 있었음.
- 루트 `settings.gradle`에 `include 'admin'` 필요.

## 테스트 패턴

gateway와 동일 (`docs/gateway.md` 참고). `admin/src/test/java/.../config/TestSecurityConfig.java`로 전체 `permitAll` 처리 후 `mockAuthentication`으로 유저 ID 주입. ADMIN 재검증 로직이 있는 컨트롤러는 `userRepository.findById(uid)`에 `role("ADMIN")`/`role("USER")` 유저를 각각 스텁해 200/403 케이스를 모두 검증한다.

## 남은 작업

- `app.jwt.expiration-days`가 admin(1일)과 gateway(30일)에서 다름 — 의도적인지 확인 필요.
- 상품 삭제는 승인 대기(`isActive=false`) 상태에서만 허용. 이미 승인된 상품을 완전히 내리는 기능은 없음 (필요 시 별도 엔드포인트 논의).
