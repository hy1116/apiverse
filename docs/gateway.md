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
| ANY | `/gateway/{productId}/**` | X-API-KEY 헤더 |

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
- ADMIN 확인: `userRepository.findById(uid)` 후 `"ADMIN".equals(user.getTier())`

## 프록시 흐름 (ProxyService)

```
X-API-KEY 헤더 → ApiKey 조회 → isActive/productId/IP 화이트리스트 검증
→ 월 쿼터 확인 (monthlyQuota=-1 무제한)
→ Redis Token Bucket (callsPerSec 기준, 초과 시 429)
→ WebClient 포워드 (업스트림 상태 코드 그대로)
→ BillingLog 저장 + used_quota++ (fire-and-forget)
```

Token Bucket Redis 키: `rate_limit:{apiKeyValue}`. 밀리토큰(×1000) 저장 — Redis float 정밀도 문제 회피.

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
