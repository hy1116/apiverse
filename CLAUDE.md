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
├── core/                  ← 공유 라이브러리: R2DBC 엔티티 + 레포지토리
├── gateway/               ← Spring Boot 앱: WebFlux, Security, JWT, Redis
├── front/                 ← React 18 + Vite 5 + Tailwind CSS 3 SPA
├── docker-compose.yml     ← PostgreSQL + pgAdmin
└── docker/pgadmin/
```

### core
Plain Java library (no Spring Boot plugin). Contains:
- **Entities** (`com.hypepia.apiverse.core.entity`): `User`, `ApiProduct`, `ApiKey`, `BillingLog` — `@Table` R2DBC mappings. Multi-word columns use `@Column("snake_case")`.
- **Repositories** (`com.hypepia.apiverse.core.repository`): `ReactiveCrudRepository` interfaces with custom query methods.
- **Config** (`com.hypepia.apiverse.core.config`): `R2dbcRepositoryConfig` — hosts `@EnableR2dbcRepositories`.

### gateway
Runnable Spring Boot app. `@SpringBootApplication(scanBasePackages = "com.hypepia.apiverse")` scans both modules.

**Package layout:**
| Package | Class | Purpose |
|---|---|---|
| `config` | `SecurityConfig` | WebFlux Security: JWT filter, per-path auth rules |
| `config` | `JwtUtils` | HMAC-SHA256 token generation / parsing (`io.jsonwebtoken` 0.12.6) |
| `config` | `JwtWebFilter` | Extracts `Authorization: Bearer` → sets `UsernamePasswordAuthenticationToken` in ReactiveSecurityContextHolder |
| `auth` | `AuthController` | `POST /api/auth/signup`, `POST /api/auth/login`, `GET /api/auth/check-email` |
| `product` | `ApiProductController` | `GET /api/products`, `GET /api/products/{id}`, `GET /api/products/{id}/my-key` |
| `key` | `ApiKeyController` | `GET /api/keys`, `POST /api/keys`, `DELETE /api/keys/{id}` |
| `usage` | `UsageController` | `GET /api/usage/daily` |

**Resources:**
- `schema.sql` — DDL, runs on startup (`spring.sql.init.mode=always`), idempotent via `IF NOT EXISTS` + `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`.
- `data.sql` — dev seed data: 4 api_products (with full `spec_json`), 1 user (`dev@hypepia.com`), 2 api_keys, 608 billing_log records over 7 days. All inserts guarded by `ON CONFLICT ... DO NOTHING`.
- `application.properties` — env var overrides: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `JWT_SECRET`.
- `db/migration/` — Flyway 제거 후 남은 파일들. `spring.sql.init`을 사용하므로 읽히지 않음. 삭제해도 무방.

### front
React 18 SPA (Vite 5, Tailwind CSS 3, Axios, Recharts, SwaggerUI).

**Key conventions:**
- `src/api/client.js` — Axios instance, baseURL `/api`. Request interceptor injects `Authorization: Bearer {token}` from `localStorage('av_user')`. Response interceptor redirects to `/login` on 401.
- `src/context/AuthContext.jsx` — stores `{ id, email, companyName, tier, token }` in `localStorage('av_user')`.
- `src/context/ThemeContext.jsx` — toggles `html.dark` class for Tailwind dark mode.
- `src/data/mockData.js` — static reference data only (`MOCK_USER`, `mockApiProducts`, `mockUsageData`). Not used in production paths.
- Tailwind dark mode: `darkMode: 'class'` in config. SwaggerUI dark overrides live in `src/index.css`.
- Font: Nanum Gothic (Google Fonts) applied globally and to `.swagger-ui`.

**Routes:**
```
/login                → LoginPage
/signup               → SignupPage
/dashboard            → DashboardPage (ProtectedRoute)
/marketplace          → MarketplacePage (ProtectedRoute)
/marketplace/:id      → ApiDetailPage (ProtectedRoute)
/marketplace/register → RegisterApiPage (ProtectedRoute)
```

Vite dev server proxies `/api/*` to `http://127.0.0.1:8080` (IPv4 explicit to avoid Node.js IPv6 resolution).

## Key design decisions

**All controllers must be non-blocking** — return `Mono<T>` / `Flux<T>`. Never use blocking calls inside reactive chains.

**JWT auth flow:**
1. `POST /api/auth/signup` or `POST /api/auth/login` returns `{ id, email, companyName, tier, token }`.
2. Frontend stores in `localStorage('av_user')`; Axios injects as `Bearer` header on every request.
3. `JwtWebFilter` validates token, puts `userId` (Long) as principal in security context.
4. Controllers extract userId via `ReactiveSecurityContextHolder.getContext().map(ctx -> (Long) ctx.getAuthentication().getPrincipal())`.

**Endpoint auth rules (SecurityConfig):**
- `permitAll`: `/api/auth/**`, `GET /api/products`, `GET /api/products/*`, `/api/usage/**`
- `authenticated`: everything else (keys, `GET /api/products/{id}/my-key`, etc.)
- `GET /api/products/{id}/my-key` returns `{}` when unauthenticated (graceful, no 401) because the security context is simply empty.

**R2DBC specifics:**
- No JPA, no JOINs from ORM. Cross-entity enrichment is done by chaining `flatMap` calls (e.g., `ApiKeyController` fetches product name separately after saving a key).
- `ApiProduct.specJson` uses `@JsonRawValue` so the stored TEXT is serialized as an embedded JSON object (not an escaped string) in API responses — required for SwaggerUI to parse it.

**Schema evolution:** Production DB changes are applied manually. `schema.sql` uses `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` for backwards-compatible column additions on existing databases.

**Password hashing:** BCryptPasswordEncoder (Spring Security). Login verifies with `passwordEncoder.matches()`.

**Sandbox key format:** `apiverse_sandbox_{32-char-uuid-no-dashes}` — generated in `ApiKeyController`, stored in `api_keys.api_key_value`.

## DB Schema

```
users           id, email (UNIQUE), password_hash, company_name, phone, tier, created_at
api_products    id, name (UNIQUE), description, base_url, is_premium, is_active, category, calls_per_sec, spec_json
api_keys        id, user_id → users, api_product_id → api_products, api_key_value (UNIQUE), white_list_ip, monthly_quota (-1 = unlimited), used_quota, is_active, created_at
billing_logs    id, api_key_value, request_path, http_method, response_status, client_ip, request_time
```

Index: `idx_api_key_value ON api_keys(api_key_value)`, `api_products_name_unique ON api_products(name)`.
