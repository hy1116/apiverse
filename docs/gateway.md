# gateway 모듈

Spring Boot 앱. `@SpringBootApplication(scanBasePackages = "com.hypepia.apiverse")`로 core 포함 스캔.

## 패키지 구조

```
gateway/
├── config/     SecurityConfig, JwtUtils, JwtWebFilter
├── auth/       AuthController
├── product/    ApiProductController, RegisterProductRequest
├── key/        ApiKeyController
├── usage/      UsageController
├── inquiry/    InquiryController, InquiryRequest, AnswerRequest
├── proxy/      ProxyController, ProxyService, RateLimiter, ProxyConfig
└── scheduler/  QuotaResetScheduler
```

요청 DTO(`XxxRequest`)는 해당 기능 패키지에 위치. core에 두지 않음 (core가 HTTP에 의존하면 안 됨).

## 엔드포인트

| 메서드 | 경로 | 인증 |
|---|---|---|
| POST | `/api/auth/signup`, `/api/auth/login` | 불필요 |
| GET | `/api/auth/check-email` | 불필요 |
| GET | `/api/products` | 불필요 |
| GET | `/api/products/{id}` | 불필요 |
| GET | `/api/products/{id}/my-key` | 선택 (비로그인 시 `{}`) |
| POST | `/api/products` | 필요 (isActive=false로 저장, 승인 대기) |
| GET | `/api/products/pending` | ADMIN |
| PATCH | `/api/products/{id}/approve` | ADMIN |
| GET/POST/DELETE | `/api/keys`, `/api/keys/{id}` | 필요 |
| GET | `/api/usage/daily` | 필요 (본인 키 데이터만) |
| POST/GET/DELETE | `/api/inquiries`, `/api/inquiries/{id}` | 필요 (본인 것만) |
| POST | `/api/inquiries/{id}/answer` | ADMIN |
| ANY | `/gateway/{code}/**` | X-API-KEY 헤더 |

## Security 경로 규칙

```
permitAll       : /api/auth/**, GET /api/products, GET /api/products/*, /gateway/**
authenticated   : GET /api/products/pending  ← 반드시 /api/products/* 보다 앞에 선언
authenticated   : 그 외 모든 경로 (catch-all)
```

`/api/products/pending`을 `/api/products/*` permitAll 뒤에 두면 먼저 매칭되어 허가됨 — 순서가 핵심.

## 인증

- `JwtWebFilter`: `Authorization: Bearer` 추출 → `UsernamePasswordAuthenticationToken(userId)` → SecurityContext
- 컨트롤러: `@AuthenticationPrincipal Mono<Long> principal` 파라미터로 userId 추출. `ReactiveSecurityContextHolder` 직접 사용 금지.
- ADMIN 확인: `userRepository.findById(uid)` 후 `"ADMIN".equals(user.getRole())` (tier는 과금 등급 전용, role은 권한 전용 — [[role-tier-분리]] 참고)

## 프록시 흐름 (ProxyService)

```
/gateway/{code}/** → api_products.code로 상품 조회 (product_id 대신 사용 — DB PK 비노출)
→ X-API-KEY 헤더 → ApiKey 조회 → isActive/apiProductId/IP 화이트리스트 검증
→ 월 쿼터 확인 (monthlyQuota=-1 무제한)
→ Redis Token Bucket (callsPerSec 기준, 초과 시 429)
→ WebClient 포워드 (product.upstreamApiKey가 설정돼 있으면 upstreamKeyParam 위치에 주입 후 업스트림 상태 코드 그대로 반환)
→ BillingLog 저장 + used_quota++ (fire-and-forget)
```

### 업스트림 타임아웃

`ProxyConfig.webClientBuilder()`는 Reactor Netty `HttpClient`에 connect 5초 / response 10초 타임아웃을 설정하고, `ProxyService.forward()`도 `.timeout(Duration.ofSeconds(15))`로 한 번 더 감싼다. 업스트림이 TCP 연결만 받고 응답을 주지 않는 경우(예: 방화벽에 막혀 SYN에 응답 없음) 타임아웃 설정이 없으면 요청이 끝없이 대기하게 된다 — 실제로 seed 데이터의 `juso` 상품 `base_url`(`https://api.juso.go.kr/v2`, 실제 서비스 도메인이 아닌 placeholder)로 테스트하다가 무한 로딩이 발생해 추가됨. 타임아웃 초과 시 504 Gateway Timeout으로 응답한다.

Token Bucket Redis 키: `rate_limit:{apiKeyValue}`. 밀리토큰(×1000) 저장 — Redis float 정밀도 문제 회피.

### 업스트림 API 키 주입

일부 업스트림(공공데이터포털 등)은 자체 인증키를 요구한다. `api_products.upstream_api_key` + `upstream_key_param`(형식: `header:{헤더명}` 또는 `query:{파라미터명}`, 예: `query:serviceKey`)에 설정해두면 `ProxyService.applyUpstreamKey()`가 업스트림 호출 시 해당 위치에 키를 실어 보낸다. 두 값 다 비어있으면 아무것도 주입하지 않음 (무료 공개 API 등).

`upstreamApiKey`/`upstreamKeyParam`은 `ApiProduct` 엔티티에서 `@JsonIgnore` 처리되어 있어 `/api/products`, `/api/products/{id}` 같은 공개 엔드포인트로는 절대 노출되지 않는다. admin 모듈의 `ProductAdminController`만 Map 기반 응답으로 명시적으로 값을 담아 반환한다 (`docs/admin.md` 참고).

### IP 화이트리스트

`api_keys.white_list_ip`가 비어있으면 제한 없음. 값이 있으면 `ProxyService.isIpAllowed()`가 콤마로 구분된 목록(예: `1.2.3.4,5.6.7.8`) 중 하나와 클라이언트 IP가 일치하는지 확인하고, 불일치 시 403 "IP not whitelisted"를 반환한다. 클라이언트 IP는 `X-Forwarded-For`(첫 번째 값) 우선, 없으면 소켓의 remote address를 사용(`resolveClientIp()`). admin 모듈의 `PATCH /api/admin/keys/{id}/whitelist-ip`로 값을 설정한다 (`docs/admin.md` 참고).

### code 생성 (상품 등록 시)

`ApiProductController.register()`가 자동으로 `code`를 생성한다(`generateUniqueCode()`):
1. **base_url 도메인 우선** — `extractDomainSlug()`가 호스트명에서 `www`/`api` 같은 흔한 서브도메인은 건너뛰고 다음 라벨을 사용한다. 예: `https://api.juso.go.kr/v2` → `juso`, `https://api.krx-data.co.kr/v1` → `krx-data`.
2. 도메인에서 뽑을 수 없으면(URL 파싱 실패 등) 상품명을 슬러그화(`slugify()`, 한글 유지·공백/특수문자는 하이픈)한 값으로 대체.
3. 중복 시 `-2`, `-3` 접미사로 유니크를 보장(`findUniqueCode()`).

관리자가 승인 후 `PATCH /api/admin/products/{id}`로 직접 수정할 수 있다.

### responseType

`api_products.response_type`은 업스트림 응답 형식을 안내하기 위한 필드(`JSON`/`XML`/`TEXT`, 기본값 `JSON`)로, 등록 시(`RegisterProductRequest.responseType()`) 지정하지 않으면 `"JSON"`으로 저장된다. 실제 응답 바디를 파싱/변환하지는 않고 — `ProxyService`는 업스트림 응답을 항상 문자열 그대로 전달 — `web`/`console`에서 사용자에게 표시하는 용도로만 쓰인다.

## 중첩 flatMap 제네릭 추론 오류

```java
// 컴파일 오류: ResponseEntity.notFound().build()는 항상 ResponseEntity<Void>
return principal.flatMap(uid ->
    repo.findById(id).flatMap(e -> Mono.just(ResponseEntity.ok(e)))
        .defaultIfEmpty(ResponseEntity.notFound().build()));

// 올바른 패턴: 모든 flatMap에 명시적 타입, status() 사용
return principal.<ResponseEntity<Entity>>flatMap(uid ->
    repo.findById(id).<ResponseEntity<Entity>>flatMap(e ->
        Mono.just(ResponseEntity.ok(e)))
    .defaultIfEmpty(ResponseEntity.<Entity>status(HttpStatus.NOT_FOUND).build()));
```

## 테스트 패턴

```java
@WebFluxTest(controllers = XxxController.class)  // 패키지: org.springframework.boot.webflux.test.autoconfigure
@Import(TestSecurityConfig.class)                 // 모든 경로 permitAll
class XxxControllerTest {
    // 인증이 필요한 엔드포인트:
    webTestClient.mutateWith(mockAuthentication(
        new UsernamePasswordAuthenticationToken(1L, null, List.of())))
}
```
