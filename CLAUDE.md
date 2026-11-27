# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build all modules
./gradlew build

# 로컬 인프라 (PostgreSQL, pgAdmin, Redis, Kafka, kafka-ui)
docker-compose up -d

# Run apps — gateway를 먼저 띄워야 schema.sql이 적용된다 (아래 "스키마 소유권" 참고)
./gradlew :gateway:bootRun
./gradlew :admin:bootRun
./gradlew :event-consumer:bootRun
./gradlew :batch:bootRun --args="--targetMonth=2026-11"

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

On Windows, use `gradlew.bat` instead of `./gradlew`. 개발용 시드 데이터는 `gateway/src/main/resources/data.sql`, API별 curl 예시와 시드 계정은 [docs/TEST_GUIDE.md](docs/TEST_GUIDE.md) 참고.

### k8s (로컬 kind)
```bash
./k8s/deploy.sh up                 # 이미지 빌드 → kind 로드 → ingress-nginx → kubectl apply -k k8s/ → 롤아웃 대기
./k8s/deploy.sh status
./k8s/deploy.sh down [--wipe-data] # --wipe-data는 postgres PVC/namespace까지 삭제
```
- Docker Desktop 내장 k8s는 쓰지 않는다 (빌드 이미지와 containerd가 분리됨) — kind/minikube/OrbStack. 자세한 내용은 [k8s/README.md](k8s/README.md).
- JVM 모듈(gateway/admin/event-consumer/batch) Dockerfile은 **저장소 루트를 빌드 컨텍스트**로 쓴다(core 의존). web/console은 각자 디렉터리가 컨텍스트.
- JVM Dockerfile은 BuildKit 캐시 마운트 + layered jar + `eclipse-temurin:17-jre-alpine`. 기본 `JAVA_TOOL_OPTIONS`(MaxRAMPercentage=70, SerialGC)는 k8s env로 덮어쓸 수 있다.

## Architecture

Multi-module Gradle project using the **reactive stack** (WebFlux + R2DBC), targeting Java 17. Frontends are separate Vite apps under `web/` (user) and `console/` (admin).

```
apiverse/
├── core/                  ← 공유 라이브러리: R2DBC 엔티티 + 레포지토리 + DB 프로젝션
├── gateway/               ← Spring Boot 앱 (8080): 사용자 API, WebFlux, Security, JWT, Redis, Proxy
├── admin/                 ← Spring Boot 앱 (8090): 어드민 전용 API
├── event-consumer/        ← Spring Boot 앱 (웹 서버 없음): Kafka billing-log 토픽 → billing_logs 배치 저장
├── batch/                 ← Spring Batch 앱 (JDBC, 실행 후 종료): 월 정산 등 — k8s CronJob으로 실행
├── web/                   ← React 18 + Vite 5 + Tailwind CSS 3 SPA (www.apiverse.com)
├── console/               ← React 18 + Vite 5 + Tailwind CSS 3 어드민 SPA (admin.apiverse.com)
├── docs/                  ← 모듈별 상세 분석 문서
├── k8s/                   ← kustomize 매니페스트 + deploy.sh (모듈별 디렉터리, kafka/postgres/redis 포함)
├── docker-compose.yml     ← 로컬 인프라: PostgreSQL, pgAdmin, Redis, Kafka(KRaft), kafka-ui
└── docker/pgadmin/
```

### 요청/로그 흐름 (여러 모듈에 걸침)
```
client ─X-API-KEY─▶ gateway /gateway/{code}/**  (ProxyService)
   blocked_ips 확인 → API 키/화이트리스트/쿼터 검증 → Redis 토큰버킷(RateLimiter, Lua) → 업스트림 포워드
   └─ 응답 후 BillingLogEvent ─▶ Kafka "billing-log" ─▶ event-consumer (bufferTimeout 배치) ─▶ billing_logs
        (Kafka 발행 실패 시 gateway가 R2DBC로 billing_logs에 직접 저장 — 폴백)
gateway/admin 의 REST 요청(/gateway/** 제외) ─ AccessLogWebFilter ─▶ access_logs (source = GATEWAY | ADMIN)
batch (매월) : billing_logs + api_keys ─▶ api_key_monthly_usage, used_quota 차감
```
- Kafka 이벤트 DTO와 (역)직렬화기·토픽 상수는 `core/kafka/`에 있어 producer(gateway)와 consumer(event-consumer)가 공유한다.
- event-consumer에 새 토픽을 추가할 때는 `app.kafka.<topic>.*` 프로퍼티 블록과 컨슈머 클래스를 추가한다 (브로커 주소 `app.kafka.bootstrap-servers`는 공유).

### 스키마 소유권
업무 테이블 DDL은 **`gateway/src/main/resources/schema.sql` 한 곳**에만 있다 (`spring.sql.init.mode=always`, `CREATE ... IF NOT EXISTS` + `ALTER ... ADD COLUMN IF NOT EXISTS`로 멱등). admin·event-consumer·batch는 `spring.sql.init.mode=never` — 테이블/컬럼 변경은 반드시 gateway의 schema.sql에서 한다. batch의 `BATCH_*` 메타 테이블만 batch가 자체 생성.

**모듈별 상세 문서:**
- [core 분석](docs/core.md) — 엔티티, 레포지토리, DB 스키마, R2DBC 주의사항
- [gateway 분석](docs/gateway.md) — 전체 엔드포인트, 인증 구조, 프록시 흐름, 리액티브 패턴
- [web 분석](docs/web.md) — 라우트, 컴포넌트, 인증/에러 처리
- [admin 분석](docs/admin.md) — 어드민 API 엔드포인트, 인증 구조
- [console 분석](docs/console.md) — 어드민 라우트, 컴포넌트

## Module Summary

### core
Plain Java library (no Spring Boot plugin). Contains:
- **Entities** (`entity/`): `User`, `ApiProduct`, `ApiKey`, `BillingLog`, `Inquiry`, `BlockedIp`, `AccessLog`
- **Repositories** (`repository/`): `ReactiveCrudRepository` 인터페이스
- **Projection** (`projection/`): `DailyStat`, `QuotaUsageStat`, `StatusCodeStat` 등 — 레포지토리 `@Query` 결과 매핑
- **Kafka** (`kafka/`): `BillingLogEvent` + Serializer/Deserializer, `BillingLogTopics`
- **Config** (`config/`): `R2dbcRepositoryConfig` — `@EnableR2dbcRepositories`

### gateway
Runnable Spring Boot app. `@SpringBootApplication(scanBasePackages = "com.hypepia.apiverse")`로 core 포함 스캔.

| 패키지 | 주요 클래스 | 역할 |
|---|---|---|
| `config` | `SecurityConfig`, `JwtUtils`, `JwtWebFilter`, `KafkaProducerConfig`, `OpenApiConfig` | WebFlux Security, JWT 생성/검증/필터, Kafka sender, springdoc |
| `logging` | `AccessLogWebFilter`, `AccessLoggingConfig` | REST 접근 로그 → `access_logs` (`/gateway/**`는 billing_logs가 담당하므로 제외) |
| `auth` | `AuthController` | 회원가입, 로그인, 이메일 중복 확인 |
| `profile` | `ProfileController`, `UpdateProfileRequest` | 내 정보 조회/수정 (companyName/phone) |
| `product` | `ApiProductController`, `RegisterProductRequest` | 상품 CRUD + 승인 플로우 |
| `key` | `ApiKeyController` | API 키 발급/목록/폐기 |
| `usage` | `UsageController` | 7일 일별 호출 통계 (로그인 유저 본인 것만) |
| `inquiry` | `InquiryController`, `InquiryRequest`, `AnswerRequest` | 문의 CRUD + 관리자 답변 |
| `proxy` | `ProxyController`, `ProxyService`, `RateLimiter`, `ProxyConfig` | API 키 인증 → 레이트 리밋 → 업스트림 포워드 |

### event-consumer
웹 서버 없는 Spring Boot 앱(`web-application-type=none`). core 의존. `reactor-kafka` `KafkaReceiver`로 `billing-log` 토픽을 읽어 `bufferTimeout(batch-max-size, batch-max-time)` 단위로 R2DBC 저장 후 오프셋 ack. 컨슈머 그룹 `billing-log-consumer`.

### batch
Spring Batch 6 앱(`spring-boot-starter-batch-jdbc`). 다른 모듈과 달리 **JDBC**를 쓰고 `core`(R2DBC)에 의존하지 않는다. DB 접속은 `DB_JDBC_URL`(jdbc://), 메타 테이블(`BATCH_*`)은 기동 시 자동 생성.
- `monthlySettlementJob` (k8s CronJob `monthly-settlement`, 매월 1일 00:00 KST): `usageSnapshotStep`(chunk) — 지난달 키별 사용량을 `api_key_monthly_usage`에 upsert → `quotaResetStep`(tasklet) — `used_quota`에서 스냅샷 값만큼 차감.
- 파라미터 `targetMonth=yyyy-MM`(생략 시 지난달, KST). 지난달 외 값은 거부, 완료된 월은 재실행 불가.
- 실행: `gradlew.bat :batch:bootRun --args="--targetMonth=2026-11"` / 테스트는 embedded-postgres로 Docker 없이 실행된다.
- 스케줄링은 Spring이 아니라 k8s CronJob(`k8s/batch/monthly-settlement-cronjob.yaml`)이 담당한다. `BatchJobRunner`가 Job을 1회 실행하고 결과를 종료 코드(0/1)로 반환 → CronJob `backoffLimit` 재시도 시 Spring Batch가 같은 파라미터로 실패 스텝부터 재시작. `@Scheduled`로 정산/쿼터 초기화를 다시 넣지 말 것 (gateway 레플리카 수만큼 중복 실행됨).

### web
React 18 SPA (사용자용). 인증 필요 페이지는 `ProtectedRoute`로 보호. 미인증 `/api` 응답 401 → Axios 인터셉터가 `/login` 리다이렉트. 랜딩/로그인/회원가입/404 외 페이지는 `React.lazy`로 로드 — swagger-ui-react·recharts가 초기 번들에 들어가지 않도록 새 페이지도 lazy로 추가할 것.

### admin
Spring Boot 어드민 전용 앱 (port 8090). core 의존. `role=ADMIN` 사용자만 접근 가능. JWT/Security/AccessLog 필터는 gateway와 별도 사본(`admin/config`, `admin/logging`)이므로 한쪽을 고치면 다른 쪽도 확인할 것.

### console
React 18 SPA (어드민용). `admin-api.apiverse.com`(8090)으로 프록시.

## Key Design Decisions

- **논블로킹 필수** — 모든 컨트롤러 `Mono<T>` / `Flux<T>` 반환. 블로킹 호출 금지.
- **JWT principal** — 컨트롤러에서 `@AuthenticationPrincipal Mono<Long> principal`로 userId 추출. `ReactiveSecurityContextHolder` 직접 사용 금지.
- **ADMIN 확인** — `users.role` 컬럼(`USER`/`ADMIN`)으로 판별: `"ADMIN".equals(user.getRole())`. `tier`는 과금 등급(FREE/PRO/ENTERPRISE)이고 권한과 무관 — 둘을 섞으면 tier 변경 API로 권한 상승이 가능해지므로 분리되어 있음.
- **API 상품 등록** — 누구나 가능, `isActive=false`로 저장. 관리자가 `/api/products/{id}/approve`로 활성화.
- **프록시 인증** — `/gateway/**`는 JWT 아닌 `X-API-KEY` 헤더. SecurityConfig에서 `permitAll`, ProxyService 내부에서 검증.
- **WebFilter는 `@Component` 금지** — `JwtWebFilter`는 `SecurityConfig`에서 `new`로 체인에 추가하고, `AccessLogWebFilter`는 `AccessLoggingConfig`에서 `@Bean`으로 등록한다. `@Component`면 `@WebFluxTest` 슬라이스가 필터를 끌어와 레포지토리 의존성을 못 찾고 컨트롤러 테스트가 깨진다.
- **DTO 위치** — HTTP 요청 DTO는 `gateway` 기능 패키지. SQL 프로젝션(`DailyStat`)은 레포지토리와 같은 `core/projection/`.

비직관적인 구현 패턴은 [gateway.md](docs/gateway.md), [core.md](docs/core.md) 참고.

## DB Schema

```
users           id, email (UNIQUE), password_hash, company_name, phone, tier, role, created_at, updated_at
api_products    id, name (UNIQUE), code (UNIQUE, /gateway/{code}/** 라우팅용),
                description, base_url, is_premium, is_active,
                category, calls_per_sec, response_type ('JSON'|'XML'|'TEXT'), spec_json,
                upstream_api_key, upstream_key_param ('header:{name}'|'query:{name}'),
                created_by → users (등록자, 본인 등록 상품 진행 상황 조회용)
api_keys        id, user_id → users, api_product_id → api_products,
                api_key_value (UNIQUE), white_list_ip,
                monthly_quota (-1=무제한), used_quota, is_active, created_at
billing_logs    id, api_key_value, request_path, http_method, response_status,
                client_ip, request_time
inquiries       id, user_id → users, title, content,
                status ('PENDING'|'ANSWERED'), answer, created_at, answered_at
blocked_ips     id, ip_address (UNIQUE), reason, created_at (전역 /gateway/** 차단 목록)
access_logs     id, source ('GATEWAY'|'ADMIN'), user_id → users, request_path, http_method,
                response_status, client_ip, request_time
api_key_monthly_usage  id, usage_month (해당 월 1일), api_key_id → api_keys, user_id, api_product_id,
                monthly_quota, used_quota, over_quota, request_count, error_count, settled_at
                UNIQUE (usage_month, api_key_id) — batch 모듈이 기록
```

Index: `idx_api_key_value ON api_keys(api_key_value)`, `api_products_name_unique ON api_products(name)`, `api_products_code_unique ON api_products(code)`.
