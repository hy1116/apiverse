# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build all modules
./gradlew build

# Run the gateway (main app)
./gradlew :apiverse-gateway:bootRun

# Compile only (fast check)
./gradlew :apiverse-core:compileJava :apiverse-gateway:compileJava

# Run tests
./gradlew test

# Run a single test class
./gradlew :apiverse-gateway:test --tests "com.hypepia.apiverse.gateway.SomeTest"
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Architecture

Multi-module Gradle project using the **reactive stack** (WebFlux + R2DBC), targeting Java 17.

```
apiverse/                  ← parent build (no source)
├── apiverse-core/         ← shared library: R2DBC entities + repositories
└── apiverse-gateway/      ← Spring Boot runnable: WebFlux routing, Security filters, Redis
```

### apiverse-core
Plain Java library (no Spring Boot plugin). Contains:
- **Entities** (`com.hypepia.apiverse.core.entity`): `User`, `ApiProduct`, `ApiKey`, `BillingLog` — Spring Data R2DBC `@Table` mappings for PostgreSQL.
- **Repositories** (`com.hypepia.apiverse.core.repository`): Reactive `ReactiveCrudRepository` interfaces.
- **Config** (`com.hypepia.apiverse.core.config`): `R2dbcRepositoryConfig` — `@EnableR2dbcRepositories` lives here so gateway doesn't need the r2dbc compile dependency directly.

### apiverse-gateway
The runnable Spring Boot app. `@SpringBootApplication(scanBasePackages = "com.hypepia.apiverse")` scans both modules. Owns:
- `schema.sql` — DDL runs on startup via `spring.sql.init.mode=always` (idempotent `IF NOT EXISTS`).
- `application.properties` — R2DBC + Redis config; credentials read from env vars (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`) with localhost defaults.

### Key technology choices
- **WebFlux / Reactor** — all controllers and services must return `Mono<T>` / `Flux<T>`, not blocking types.
- **R2DBC** (not JPA) — reactive PostgreSQL driver (`org.postgresql:r2dbc-postgresql`). Multi-word column names use explicit `@Column("snake_case")` annotations.
- **Spring Security Reactive** — custom `WebFilter` chain planned for `X-API-KEY` header validation.
- **Redis** (`spring-boot-starter-data-redis-reactive`) — Token Bucket rate limiting and API Key caching (implementation in Phase 1 Week 3-4).

### DB Schema
Four tables: `users`, `api_products`, `api_keys` (with `idx_api_key_value` index), `billing_logs`. See `apiverse-gateway/src/main/resources/schema.sql`.
