# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build all modules
./gradlew build

# Run the gateway (main app)
./gradlew :gateway:bootRun

# Compile only (fast check)
./gradlew :core:compileJava :gateway:compileJava

# Run tests
./gradlew test

# Run a single test class
./gradlew :gateway:test --tests "com.hypepia.apiverse.gateway.SomeTest"

# Frontend dev server (proxy → localhost:8080)
cd web && npm run dev

# Frontend build
cd web && npm run build

# Admin frontend dev server (proxy → localhost:8090)
cd console && npm run dev
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Architecture

Multi-module Gradle project using the **reactive stack** (WebFlux + R2DBC), targeting Java 17. Frontends are separate Vite apps under `web/` (user) and `console/` (admin).

```
apiverse/
├── core/                  ← 공유 라이브러리: R2DBC 엔티티 + 레포지토리 + DB 프로젝션
├── gateway/               ← Spring Boot 앱 (8080): 사용자 API, WebFlux, Security, JWT, Redis, Proxy
├── admin/                 ← Spring Boot 앱 (8090): 어드민 전용 API
├── web/                   ← React 18 + Vite 5 + Tailwind CSS 3 SPA (www.apiverse.com)
├── console/               ← React 18 + Vite 5 + Tailwind CSS 3 어드민 SPA (admin.apiverse.com)
├── docs/                  ← 모듈별 상세 분석 문서
├── docker-compose.yml     ← PostgreSQL + pgAdmin
└── docker/pgadmin/
```

**모듈별 상세 문서:**
- [core 분석](docs/core.md) — 엔티티, 레포지토리, DB 스키마, R2DBC 주의사항
- [gateway 분석](docs/gateway.md) — 전체 엔드포인트, 인증 구조, 프록시 흐름, 리액티브 패턴
- [web 분석](docs/web.md) — 라우트, 컴포넌트, 인증/에러 처리
- [admin 분석](docs/admin.md) — 어드민 API 엔드포인트, 인증 구조
- [console 분석](docs/console.md) — 어드민 라우트, 컴포넌트

## Module Summary

### core
Plain Java library (no Spring Boot plugin). Contains:
- **Entities** (`entity/`): `User`, `ApiProduct`, `ApiKey`, `BillingLog`, `Inquiry`
- **Repositories** (`repository/`): `ReactiveCrudRepository` 인터페이스
- **Projection** (`projection/`): `DailyStat` — SQL 쿼리 결과 매핑 (`BillingLogRepository`에서 사용)
- **Config** (`config/`): `R2dbcRepositoryConfig` — `@EnableR2dbcRepositories`

### gateway
Runnable Spring Boot app. `@SpringBootApplication(scanBasePackages = "com.hypepia.apiverse")`로 core 포함 스캔.

| 패키지 | 주요 클래스 | 역할 |
|---|---|---|
| `config` | `SecurityConfig`, `JwtUtils`, `JwtWebFilter` | WebFlux Security, JWT 생성/검증/필터 |
| `auth` | `AuthController` | 회원가입, 로그인, 이메일 중복 확인 |
| `product` | `ApiProductController`, `RegisterProductRequest` | 상품 CRUD + 승인 플로우 |
| `key` | `ApiKeyController` | API 키 발급/목록/폐기 |
| `usage` | `UsageController` | 7일 일별 호출 통계 (로그인 유저 본인 것만) |
| `inquiry` | `InquiryController`, `InquiryRequest`, `AnswerRequest` | 문의 CRUD + 관리자 답변 |
| `proxy` | `ProxyController`, `ProxyService`, `RateLimiter`, `ProxyConfig` | API 키 인증 → 레이트 리밋 → 업스트림 포워드 |
| `scheduler` | `QuotaResetScheduler` | 매월 1일 used_quota 초기화 |

### web
React 18 SPA (사용자용). 인증 필요 페이지는 `ProtectedRoute`로 보호. 미인증 `/api` 응답 401 → Axios 인터셉터가 `/login` 리다이렉트.

### admin
Spring Boot 어드민 전용 앱 (port 8090). core 의존. ADMIN tier 사용자만 접근 가능.

### console
React 18 SPA (어드민용). `admin-api.apiverse.com`(8090)으로 프록시.

## Key Design Decisions

- **논블로킹 필수** — 모든 컨트롤러 `Mono<T>` / `Flux<T>` 반환. 블로킹 호출 금지.
- **JWT principal** — 컨트롤러에서 `@AuthenticationPrincipal Mono<Long> principal`로 userId 추출. `ReactiveSecurityContextHolder` 직접 사용 금지.
- **ADMIN 확인** — `users.role` 컬럼(`USER`/`ADMIN`)으로 판별: `"ADMIN".equals(user.getRole())`. `tier`는 과금 등급(FREE/PRO/ENTERPRISE)이고 권한과 무관 — 둘을 섞으면 tier 변경 API로 권한 상승이 가능해지므로 분리되어 있음.
- **API 상품 등록** — 누구나 가능, `isActive=false`로 저장. 관리자가 `/api/products/{id}/approve`로 활성화.
- **프록시 인증** — `/gateway/**`는 JWT 아닌 `X-API-KEY` 헤더. SecurityConfig에서 `permitAll`, ProxyService 내부에서 검증.
- **DTO 위치** — HTTP 요청 DTO는 `gateway` 기능 패키지. SQL 프로젝션(`DailyStat`)은 레포지토리와 같은 `core/projection/`.

비직관적인 구현 패턴은 [gateway.md](docs/gateway.md), [core.md](docs/core.md) 참고.

## DB Schema

```
users           id, email (UNIQUE), password_hash, company_name, phone, tier, role, created_at
api_products    id, name (UNIQUE), code (UNIQUE, /gateway/{code}/** 라우팅용),
                description, base_url, is_premium, is_active,
                category, calls_per_sec, response_type ('JSON'|'XML'|'TEXT'), spec_json,
                upstream_api_key, upstream_key_param ('header:{name}'|'query:{name}')
api_keys        id, user_id → users, api_product_id → api_products,
                api_key_value (UNIQUE), white_list_ip,
                monthly_quota (-1=무제한), used_quota, is_active, created_at
billing_logs    id, api_key_value, request_path, http_method, response_status,
                client_ip, request_time
inquiries       id, user_id → users, title, content,
                status ('PENDING'|'ANSWERED'), answer, created_at, answered_at
```

Index: `idx_api_key_value ON api_keys(api_key_value)`, `api_products_name_unique ON api_products(name)`, `api_products_code_unique ON api_products(code)`.
