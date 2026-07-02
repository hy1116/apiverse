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
└── usage/      UsageAdminController
```

요청 DTO(`XxxRequest`)는 gateway와 동일하게 해당 기능 패키지에 위치.

## 엔드포인트

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/admin/auth/login` | role이 ADMIN 아니면 403 |
| GET | `/api/admin/products` | 전체 상품 목록 |
| GET | `/api/admin/products/pending` | 승인 대기 상품 목록 |
| PATCH | `/api/admin/products/{id}/approve` | 상품 승인 (isActive=true) |
| DELETE | `/api/admin/products/{id}/reject` | 승인 대기 상품 반려(삭제). 이미 승인된 상품은 409 |
| PATCH | `/api/admin/products/{id}` | 상품 수정 (description/baseUrl/category/callsPerSec/isPremium/specJson, null 필드는 무시) |
| GET | `/api/admin/inquiries` | 전체 문의 목록 |
| GET | `/api/admin/inquiries/{id}` | 문의 상세 |
| POST | `/api/admin/inquiries/{id}/answer` | 답변 등록 (status→ANSWERED) |
| GET | `/api/admin/users` | 전체 회원 목록 (password_hash 제외) |
| GET | `/api/admin/users/{id}` | 회원 상세 |
| PATCH | `/api/admin/users/{id}/tier` | 회원 tier 변경 |
| GET | `/api/admin/keys` | 전체 API 키 목록 (userEmail/apiProductName 포함) |
| DELETE | `/api/admin/keys/{id}` | API 키 강제 폐기 (소유자 무관) |
| GET | `/api/admin/usage/daily` | 전체 유저 대상 7일 일별 요청/에러 통계 |

`/api/admin/auth/login`만 `permitAll`, 나머지는 `authenticated()`.

## role vs tier

`users` 테이블은 두 컬럼을 분리해서 쓴다:
- `tier` — 과금 등급 (`FREE`/`PRO`/`ENTERPRISE`). 서비스 요금제와만 관련.
- `role` — 권한 (`USER`/`ADMIN`). 관리자 여부만 나타냄.

원래는 `tier`에 `ADMIN` 값을 끼워 넣어 사용했으나, 이 경우 회원 tier 변경 API(`PATCH /api/admin/users/{id}/tier`) 하나로 아무 계정이나 관리자로 승격시킬 수 있는 권한 상승 경로가 생겨 `role` 컬럼으로 분리했다. `role`을 바꾸는 API는 의도적으로 만들지 않았다 — 관리자 계정은 DB에서 직접 생성한다 (`gateway/src/main/resources/data.sql`의 `admin@apiverse.com` 시드 참고).

## 인증 및 권한 재검증

- `JwtWebFilter`는 토큰 유효성만 검증하고 role은 확인하지 않는다 — 로그인 시점에만 `AdminAuthController`가 role을 확인.
- 따라서 **모든 컨트롤러가 매 요청마다 자체적으로 ADMIN role을 재검증**해야 한다. 각 컨트롤러에 `requireAdmin(Long uid)` private 헬퍼를 두고 `UserRepository.findById`로 조회 후 role 비교 (gateway의 `InquiryController.answer()`와 동일한 인라인 패턴). 새로운 admin 컨트롤러를 추가할 때 이 체크를 빠뜨리면 로그인한 일반 유저도 접근 가능해지므로 반드시 포함할 것.

## core에 admin 전용으로 추가된 레포지토리 메서드

- `UserRepository.findAllByOrderByIdDesc()`
- `ApiKeyRepository.findAllByOrderByIdDesc()`
- `ApiProductRepository.findAllByOrderByIdDesc()`
- `InquiryRepository.findAllByOrderByCreatedAtDesc()`
- `BillingLogRepository.findDailyStatsGlobal()` — `findDailyStatsByUserId`와 동일 쿼리에서 `user_id` 필터만 제거

## 빌드 관련 주의사항

- `admin`은 `core`를 `implementation project(':core')`로 의존하지만, `core`가 `spring-boot-starter-data-r2dbc`를 `implementation`으로 선언하고 있어 **전이(transitive)되지 않는다**. `admin/build.gradle`에 `spring-boot-starter-data-r2dbc` + `r2dbc-postgresql`을 직접 선언해야 `ReactiveCrudRepository` 등이 컴파일된다. gateway는 `spring-boot-starter-data-redis-reactive`를 직접 선언하고 있어 그 전이 의존성으로 우연히 spring-data-commons를 받고 있었을 뿐, 같은 문제를 안고 있었음.
- 루트 `settings.gradle`에 `include 'admin'` 필요.

## 테스트 패턴

gateway와 동일 (`docs/gateway.md` 참고). `admin/src/test/java/.../config/TestSecurityConfig.java`로 전체 `permitAll` 처리 후 `mockAuthentication`으로 유저 ID 주입. ADMIN 재검증 로직이 있는 컨트롤러는 `userRepository.findById(uid)`에 `role("ADMIN")`/`role("USER")` 유저를 각각 스텁해 200/403 케이스를 모두 검증한다.

## 남은 작업

- `app.jwt.expiration-days`가 admin(1일)과 gateway(30일)에서 다름 — 의도적인지 확인 필요.
- 상품 삭제는 승인 대기(`isActive=false`) 상태에서만 허용. 이미 승인된 상품을 완전히 내리는 기능은 없음 (필요 시 별도 엔드포인트 논의).
