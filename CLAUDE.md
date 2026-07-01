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
cd front && npm run dev

# Frontend build
cd front && npm run build
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Architecture

Multi-module Gradle project using the **reactive stack** (WebFlux + R2DBC), targeting Java 17. Frontend is a separate Vite app under `front/`.

```
apiverse/
├── core/                  ← 공유 라이브러리: R2DBC 엔티티 + 레포지토리 + DTO
├── gateway/               ← Spring Boot 앱: WebFlux, Security, JWT, Redis, Proxy
├── front/                 ← React 18 + Vite 5 + Tailwind CSS 3 SPA
├── docker-compose.yml     ← PostgreSQL + pgAdmin
└── docker/pgadmin/
```

### core
Plain Java library (no Spring Boot plugin). Contains:
- **Entities** (`com.hypepia.apiverse.core.entity`): `User`, `ApiProduct`, `ApiKey`, `BillingLog`, `Inquiry` — `@Table` R2DBC mappings. Multi-word columns use `@Column("snake_case")`.
- **Repositories** (`com.hypepia.apiverse.core.repository`): `ReactiveCrudRepository` interfaces with custom query methods.
- **DTOs** (`com.hypepia.apiverse.core.dto`): `DailyStat`, `InquiryRequest`, `AnswerRequest` — request/response data classes.
- **Config** (`com.hypepia.apiverse.core.config`): `R2dbcRepositoryConfig` — hosts `@EnableR2dbcRepositories`.

### gateway
Runnable Spring Boot app. `@SpringBootApplication(scanBasePackages = "com.hypepia.apiverse")` scans both modules.

**Package layout:**
| Package | Class | Purpose |
|---|---|---|
| `config` | `SecurityConfig` | WebFlux Security: JWT filter, per-path auth rules |
| `config` | `JwtUtils` | HMAC-SHA256 token generation / parsing (`io.jsonwebtoken` 0.12.6) |
| `config` | `JwtWebFilter` | Extracts `Authorization: Bearer` → sets `UsernamePasswordAuthenticationToken` in ReactiveSecurityContextHolder |
| `config` | `ProxyConfig` | Beans: `WebClient.Builder`, `RedisScript<Long>` (token bucket Lua script) |
| `auth` | `AuthController` | `POST /api/auth/signup`, `POST /api/auth/login`, `GET /api/auth/check-email` |
| `product` | `ApiProductController` | `GET /api/products`, `GET /api/products/{id}`, `GET /api/products/{id}/my-key` |
| `key` | `ApiKeyController` | `GET /api/keys`, `POST /api/keys`, `DELETE /api/keys/{id}` |
| `usage` | `UsageController` | `GET /api/usage/daily` |
| `proxy` | `ProxyController` | `ANY /gateway/{productId}/**` — validates API key, rate limits, forwards request |
| `proxy` | `ProxyService` | API key validation → quota check → Token Bucket → WebClient proxy → billing log |
| `proxy` | `RateLimiter` | Redis Lua script execution for Token Bucket |
| `inquiry` | `InquiryController` | `POST /api/inquiries`, `GET /api/inquiries`, `GET /api/inquiries/{id}`, `DELETE /api/inquiries/{id}`, `POST /api/inquiries/{id}/answer` |

**Resources:**
- `schema.sql` — DDL, runs on startup (`spring.sql.init.mode=always`), idempotent via `IF NOT EXISTS` + `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`.
- `data.sql` — dev seed data: 4 api_products (with full `spec_json`), 1 user (`dev@hypepia.com`), 2 api_keys, 608 billing_log records over 7 days, 2 inquiries. All inserts guarded by `ON CONFLICT ... DO NOTHING` or `WHERE NOT EXISTS`.
- `lua/token_bucket.lua` — Redis Lua script for Token Bucket rate limiting. Uses integer millitokens (×1000) to avoid floating-point precision issues in Redis.
- `application.properties` — env var overrides: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `JWT_SECRET`.
- `db/migration/` — Flyway 제거 후 남은 파일들. `spring.sql.init`을 사용하므로 읽히지 않음. 삭제해도 무방.

### front
React 18 SPA (Vite 5, Tailwind CSS 3, Axios, Recharts, SwaggerUI).

**Key conventions:**
- `src/api/client.js` — Axios instance, baseURL `/api`. Request interceptor injects `Authorization: Bearer {token}` from `localStorage('av_user')`. Response interceptor redirects to `/login` on 401.
- `src/context/AuthContext.jsx` — stores `{ id, email, companyName, tier, token }` in `localStorage('av_user')`.
- `src/context/ThemeContext.jsx` — toggles `html.dark` class for Tailwind dark mode.
- `src/data/mockData.js` — static reference data only (`MOCK_USER`, `mockApiProducts`, `mockUsageData`). Not used in production paths.
- `RedirectIfAuth` (in `App.jsx`) — wraps public-only routes (`/`): authenticated users are redirected to `/dashboard`.
- Tailwind dark mode: `darkMode: 'class'` in config. SwaggerUI dark overrides live in `src/index.css`.
- Font: Nanum Gothic (Google Fonts) applied globally and to `.swagger-ui`.
- Static images: served from `front/public/`. Currently `public/imgs/dash_board.png`.

**Routes:**
```
/                         → LandingPage (public, RedirectIfAuth)
/login                    → LoginPage
/signup                   → SignupPage
/dashboard                → DashboardPage (ProtectedRoute)
/marketplace              → MarketplacePage (ProtectedRoute)
/marketplace/:id          → ApiDetailPage (public)
/marketplace/register     → RegisterApiPage (ProtectedRoute)
/inquiry                  → InquiryPage (ProtectedRoute)
```

Vite dev server proxies `/api/*` to `http://127.0.0.1:8080` (IPv4 explicit to avoid Node.js IPv6 resolution).

## Key design decisions

**All controllers must be non-blocking** — return `Mono<T>` / `Flux<T>`. Never use blocking calls inside reactive chains.

**JWT auth flow:**
1. `POST /api/auth/signup` or `POST /api/auth/login` returns `{ id, email, companyName, tier, token }`.
2. Frontend stores in `localStorage('av_user')`; Axios injects as `Bearer` header on every request.
3. `JwtWebFilter` validates token, puts `userId` (Long) as principal in security context.
4. Controllers extract userId via `@AuthenticationPrincipal Mono<Long> principal` parameter — do NOT use `ReactiveSecurityContextHolder` directly.

**Endpoint auth rules (SecurityConfig):**
- `permitAll`: `/api/auth/**`, `GET /api/products`, `GET /api/products/*`, `/api/usage/**`, `/gateway/**`
- `authenticated`: everything else (keys, `GET /api/products/{id}/my-key`, `/api/inquiries/**`, etc.)
- `GET /api/products/{id}/my-key` returns `{}` when unauthenticated (graceful, no 401).
- `/gateway/**` is `permitAll` because auth is handled inside `ProxyService` via API key header (not JWT).

**Proxy routing (`/gateway/{productId}/**`):**
1. Extract `X-API-Key` header → look up `ApiKey` entity → verify `isActive` and `apiProductId` matches.
2. Check monthly quota (`monthlyQuota == -1` means unlimited).
3. Token Bucket rate limit via Redis Lua script using `calls_per_sec` from `ApiProduct`.
4. Forward request via `WebClient.exchangeToMono` — preserves upstream status codes without throwing exceptions.
5. Write `BillingLog` fire-and-forget with `.subscribe()` after returning response.

**Token Bucket (Redis Lua):**
- Key: `rate_limit:{apiKeyValue}`
- Stores integer millitokens (capacity × 1000, consume 1000 per request) to avoid Redis float precision issues.
- Returns `1L` (allowed) or `0L` (rejected → 429 Too Many Requests).

**Inquiry feature:**
- Users can submit, list, view, and delete their own inquiries.
- `POST /api/inquiries/{id}/answer` is admin-only: checked by looking up `user.getTier().equals("ADMIN")` via `UserRepository`.
- DTOs (`InquiryRequest`, `AnswerRequest`) live in `core/dto/`.

**R2DBC specifics:**
- No JPA, no JOINs from ORM. Cross-entity enrichment is done by chaining `flatMap` calls.
- `ApiProduct.specJson` uses `@JsonRawValue` so the stored TEXT is serialized as an embedded JSON object (not an escaped string) in API responses — required for SwaggerUI to parse it.
- Java generic type inference breaks with nested `flatMap` chains returning `Mono<ResponseEntity<T>>`. Fix: add explicit type params on every `flatMap` in the chain (e.g., `.<ResponseEntity<Void>>flatMap(...)`). Use `ResponseEntity.<T>status(HttpStatus.X).build()` instead of `notFound().build()` — the latter returns `ResponseEntity<Void>` regardless of generic, causing a type mismatch.

**Schema evolution:** Production DB changes are applied manually. `schema.sql` uses `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` for backwards-compatible column additions on existing databases.

**Password hashing:** BCryptPasswordEncoder (Spring Security). Login verifies with `passwordEncoder.matches()`.

**Sandbox key format:** `apiverse_sandbox_{32-char-uuid-no-dashes}` — generated in `ApiKeyController`, stored in `api_keys.api_key_value`.

## DB Schema

```
users           id, email (UNIQUE), password_hash, company_name, phone, tier, created_at
api_products    id, name (UNIQUE), description, base_url, is_premium, is_active, category, calls_per_sec, spec_json
api_keys        id, user_id → users, api_product_id → api_products, api_key_value (UNIQUE), white_list_ip, monthly_quota (-1 = unlimited), used_quota, is_active, created_at
billing_logs    id, api_key_value, request_path, http_method, response_status, client_ip, request_time
inquiries       id, user_id → users, title, content, status ('PENDING'|'ANSWERED'), answer, created_at, answered_at
```

Index: `idx_api_key_value ON api_keys(api_key_value)`, `api_products_name_unique ON api_products(name)`.
