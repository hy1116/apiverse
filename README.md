# 🚀 apiverse: Developer-First API Marketplace & Gateway Hub

> **"지루한 B2B 영업 미팅과 대기 시간 없이, 가입 즉시 작동하는 개발자 친화적 1-Click API 중개 플랫폼"**
>
> **apiverse**는 기존 B2B API 중개 플랫폼들의 폐쇄적인 영업 프로세스를 파괴하고, 개발자가 가입 즉시 샌드박스 키를 발급받아 테스트할 수 있는 **Developer-First SaaS 아키텍처**입니다. 오픈소스 무료 나눔 API를 시작으로 정가제 기반의 프리미엄 유료 API 상품 및 AI 부가 옵션을 유연하게 결합하는 하이브리드 시스템을 지향합니다. `Java/Spring WebFlux`와 `Python/FastAPI` 각 생태계의 강점을 결합한 폴리글랏(Polyglot) 멀티 모듈 아키텍처를 구현합니다.

---

## 💡 1. Core Principles: Developer-First UX
apiverse는 철저히 개발자의 생산성과 디버깅 편의성에 모든 초점을 맞춥니다.

* **1-Click Sandbox & Free Tier:** 신용카드 등록이나 승인 대기 프로세스 없이, 가입 즉시 테스트용 `Sandbox API Key`가 자동으로 발급되어 즉시 외부 API 호출을 테스트할 수 있습니다.
* **Embedded Playground & Swagger UI:** API 상세 페이지 내에 통합 `Swagger UI`가 내장되며, 로그인된 유저의 API Key가 자동으로 세팅되어 브라우저에서 즉시 실제 API 응답 결과를 JSON으로 받아볼 수 있습니다.
* **Ready-to-Use Code Snippets:** 동일한 API 요청에 대해 `Curl`, `Java`, `JavaScript`, `Python`, `Go` 코드를 즉시 복사해서 쓸 수 있는 다국어 코드 템플릿 탭을 제공합니다.
* **Live Request/Response Stream:** 내가 연동 중인 클라이언트 앱이 내뿜는 트래픽 로그를 웹 콘솔에서 실시간(`WebSocket`)으로 모니터링하여 디버깅 스트레스를 제로로 만듭니다.

---

## 🔄 2. Business Model & Roadmap: Free to Premium

1. **기본 모델 (무료 API 나눔):** 공공 데이터, 오픈소스 인프라 API 등 전면 무료로 제공되는 상품 위주로 론칭하여 초기 유저를 락인(Lock-in)합니다. 기본 초당 호출 제한(`Rate Limit`) 레이어만 적용하여 게이트웨이가 안정적으로 라우팅합니다.
2. **확장 모델 (유료 API 및 부가 옵션):** 등급별(Basic, Pro, Enterprise) 정가제 구독 요금제와 매칭되는 유료 프리미엄 API 상품을 도입합니다. 추가로 "실시간 트래픽 이상 징후 감지 패키지"나 "대역폭 확장 옵션"을 유료 솔루션 형태로 추가 청구할 수 있는 과금 엔진을 장착합니다.

---

## 📁 3. Project Architecture (Java + Python Polyglot Multi-Module)

대용량 트래픽 라우팅 처리부와 무거운 관리용 웹 콘솔, 그리고 데이터 처리에 특화된 파이썬 모듈의 리소스를 철저히 격리하기 위해 멀티 모듈 아키텍처로 설계합니다.

[apiverse-parent 디렉토리 구조]
apiverse/ (루트 폴더)
├── build.gradle
├── settings.gradle
├── apiverse-core/ (공통 모듈: JPA 엔티티, PostgreSQL 레포지토리 - Java)
│   └── src/main/java/com/apiverse/core/
├── apiverse-gateway/ (핵심 API 게이트웨이 라우팅 엔진 - Netty / Spring WebFlux)
│   └── src/main/java/com/apiverse/gateway/filter/ (Custom API Key & Redis 분산락 필터)
└── apiverse-py-worker/ (선택 확장 모듈: 샘플 코드 가공, 헬스체크 및 데이터 정산 워커 - Python / FastAPI)
├── app/
│   ├── main.py
│   └── services/ (코드 스니펫 생성 및 통계 가공 로직)
└── requirements.txt

---

## 🛠 4. Technology Stack Specification

* **Frontend Layer**
    * **Library/Framework:** React (v18+) (SPA 싱글 페이지 아키텍처 및 대시보드 상태 관리)
    * **Styling:** Tailwind CSS (유틸리티 퍼스트 디자인 기반 컴포넌트 고속 가시화)
    * **Visualization:** Recharts / Chart.js (실시간 API 호출 트래픽 및 요금 미터링 대시보드 구현)
* **Backend Layer (Core & Application)**
    * **Language:** Java 17 (모던 자바 명세 및 불변 객체 활용)
    * **Framework:** Spring Boot 4.x (최신 스프링 에코시스템 핵심 스펙 적용)
    * **Build Tool:** Gradle (Groovy DSL 기반 멀티 프로젝트 중앙 집중식 빌드 제어)
* **API Gateway Web Core (Reactive Engine)**
    * **Reactive Stack:** Spring WebFlux / Project Reactor (Netty 내장 아키텍처 기반 고성능 Non-blocking I/O 구현)
    * **Security Framework:** Spring Security Reactive Filter Chain (커스텀 필터를 통한 전면 보안 관제)
* **Storage & Caching Layer**
    * **Main RDBMS:** PostgreSQL (v15+) (정산 로그 데이터, 마켓 상품 정보 및 회원 영속성 관리)
    * **In-Memory NoSQL:** Redis (Token Bucket 알고리즘 기반 초고속 Rate Limiting 처리 및 API Key 캐싱)
* **Messaging & Distributed Data Pipeline (Phase 2 예정)**
    * **Message Broker:** Apache Kafka (대용량 API 사용량 텔레메트리 로그 비동기 수집용 큐 파이프라인)
* **Infrastructure & DevOps Layer (Phase 3 예정)**
    * **Containerization:** Docker (Dockerfile 멀티 스테이지 이미지 빌드)
    * **Orchestration:** Kubernetes (k3s) (홈서버 환경 경량 k8s 구축 및 HPA 기반 Pod 자동 확장 자동화 검증)

---

## 🗄️ 5. Core Database Schema (PostgreSQL Schema)

### 1. users Table (개발자 회원 테이블)
CREATE TABLE users (
id BIGSERIAL PRIMARY KEY,
email VARCHAR(100) NOT NULL UNIQUE,
password_hash VARCHAR(255) NOT NULL,
company_name VARCHAR(100),
tier VARCHAR(20) DEFAULT 'FREE',
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

### 2. api_products Table (중개 API 상품 테이블)
CREATE TABLE api_products (
id BIGSERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
description TEXT,
base_url VARCHAR(255) NOT NULL,
is_premium BOOLEAN DEFAULT FALSE,
is_active BOOLEAN DEFAULT TRUE
);

### 3. api_keys Table (API Key 및 요금 쿼터 매핑 테이블)
CREATE TABLE api_keys (
id BIGSERIAL PRIMARY KEY,
user_id BIGINT REFERENCES users(id),
api_product_id BIGINT REFERENCES api_products(id),
api_key_value VARCHAR(255) NOT NULL UNIQUE,
white_list_ip VARCHAR(50),
monthly_quota INT NOT NULL DEFAULT -1,
used_quota INT DEFAULT 0,
is_active BOOLEAN DEFAULT TRUE,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_api_key_value ON api_keys(api_key_value);

### 4. billing_logs Table (API 사용량 기록 통계 테이블)
CREATE TABLE billing_logs (
id BIGSERIAL PRIMARY KEY,
api_key_value VARCHAR(255) NOT NULL,
request_path VARCHAR(255) NOT NULL,
http_method VARCHAR(10) NOT NULL,
response_status INT NOT NULL,
client_ip VARCHAR(50),
request_time TIMESTAMP NOT NULL
);

---

## 🔒 6. Security Vulnerability & Risk Review (보안 취약점 검토 보고서)

API를 대행 중개하는 프록시 게이트웨이 특성상, 일반적인 웹 서비스보다 보안 타겟 면적이 넓습니다. 반드시 방어해야 할 5대 핵심 취약점과 대응 아키텍처입니다.

### ① API Key 무차별 대입 공격 (Brute-Force) & 도난 키 오용
* 위험 시나리오: 공격자가 무작위 문자열로 X-API-KEY 헤더를 찔러보거나, 유출된 타인의 API Key로 입점사 서버의 유료 리소스를 고갈시키는 행위.
* 방어 전략:
    1. API Key 발급 시 예측 불가능한 높은 엔트로피의 난수 체계 구현 (apiverse_live_ + UUID/SecureRandom 조합).
    2. 게이트웨이 Spring Security Filter 단에서 IP 화이트리스트 매칭 필터를 탑재하여 API Key가 유출되더라도 사전에 등록된 구매자의 서버 고정 IP가 아니면 403 Forbidden 차단.

### ② DoS / DDoS 공격을 통한 프록시 및 입점사 서버 마비
* 위험 시나리오: 특정 악성 유저가 악의적으로 초당 수만 건의 API 요청을 보내 내 게이트웨이 엔진의 스레드를 고갈시키거나, 뒤에 숨은 무료 API 나눔 입점사 서버를 무차별 다운시키는 행위.
* 방어 전략:
    1. Redis Token Bucket 아키텍처를 게이트웨이 진입로에 전면 배치하여, 유저 요금제/플랜별 허용 한도(예: Free는 5 req/sec) 초과 시 원본 API 레이어로 프록시 백패싱을 타기 전에 429 Too Many Requests로 게이트웨이 선에서 컷오프 처리.
    2. Non-blocking 기반의 Spring WebFlux(Netty) 엔진을 채택하여 소수의 워커 스레드로 대량의 대기 상태(IO Blocking) 커넥션을 효율적으로 방어.

### ③ SQL 인젝션 및 다운스트림 주입 공격 (Injection)
* 위험 시나리오: 소비자가 헤더나 쿼리 파라미터에 악성 SQL 또는 스크립트를 담아 보냈을 때, 게이트웨이가 이를 필터링 없이 그대로 원본 API 입점사 서버로 전달하여 입점사 DB가 탈취당하는 현상.
* 방어 전략:
    1. 게이트웨이는 URL 경로(Path) 및 쿼리 파라미터 변수에 대해 화이트리스트 기반 정규식 필터링을 게이트웨이 필터단에서 선제 수행.
    2. 플랫폼 자체 DB(PostgreSQL) 접근 시 100% Spring Data JPA/R2DBC를 적용하고 Named Parameter를 활용해 인젝션 원천 차단.

### ④ 입점사 원본 토큰(Target Token) 유출 위험
* 위험 시나리오: 유료 API 입점사가 우리 플랫폼에 자신의 API를 등록할 때 부여한 원본 인증 토큰(target_token)이 외부로 노출되거나 게이트웨이 DB 해킹으로 털리는 경우.
* 방어 전략:
    1. api_products 테이블의 target_token 필드는 AES-256 양방향 암호화를 적용해 DB에 저장.
    2. 복호화 키는 소스 코드에 박지 않고 k8s 배포 환경의 Secret 오브젝트나 환경 변수(ENV)를 통해 런타임에 팩토링 주입 처리.

### ⑤ 가용성 저하 및 데이터 실시간성 정합성 유실 (Race Condition)
* 위험 시나리오: 유료 API 유저가 잔여 쿼터가 1번 남은 상황에서 멀티 스레드로 초당 수백 건의 요청을 동시 다발적으로 날릴 때, 사용량 체크 로직에 레이스 컨디션이 발생하여 허용된 한도보다 훨씬 많은 API를 공짜로 호출해 버리는 취약점.
* 방어 전략:
    1. 게이트웨이가 DB의 used_quota를 매번 업데이트하는 방식은 동시성 제어가 불가능하고 성능이 저하됨.
    2. 이를 방어하기 위해 Redis Lua Script를 작성하여 "키 잔여 한도 확인 및 차감(Decrement)" 연산을 단일 원자적(Atomic) 트랜잭션으로 묶어 메모리 레벨에서 동시성 완벽 제어.

---

## 📅 7. 3-Phase Tactical Execution Blueprint (점진적 빌드업 계획)

### 🛠️ Phase 1: 로컬 모놀리식 핵심 기능 완성 (1개월 차)
* [Week 1-2] 멀티 모듈 및 DB 뼈대 구축:
    - settings.gradle 및 부모 build.gradle 구성을 통해 apiverse-core와 apiverse-gateway 모듈 기본 아키텍처 바인딩.
    - PostgreSQL 설치 후 users, api_products, api_keys, billing_logs 스키마 빌드 및 고속 인덱스 설정.
* [Week 3-4] 커스텀 보안 필터 및 WebFlux 라우팅 엔진 개발:
    - Spring Security 기반 WebFilter 구현: 헤더 X-API-KEY 추출 및 로컬 Redis 스냅샷 검증 매핑 (IP 화이트리스트 필터링 연동).
    - Redis Token Bucket 루틴을 적용해 무료 나눔 상품과 유료 상품의 초당 처리량 제한 제어.
    - React 클라이언트 대시보드 화면 연동: 회원가입, 샌드박스 API Key 즉시 발급 창 및 오픈 API 상점 내 통합 Swagger UI 구동 테스트 완료.
    - (임시) API 호출 로그는 Kafka 없이 게이트웨이에서 비동기 리액티브 체인(Mono.fromRunnable)을 통해 PostgreSQL에 동기식 직접 적재 처리.

### 📈 Phase 2: Python 모듈 도입 및 데이터 파이프라인 분리 (2개월 차)
* [Week 5-6] Python (FastAPI) 모듈 이식:
    - apiverse-py-worker 모듈 신설 및 파이썬 가상환경 빌드업.
    - API 상세 정보에 띄워줄 Curl, Java, Python 언어별 샘플 코드 템플릿 스트링 가공 가속기 구현.
    - 아웃바운드 호출을 활용한 입점사 원본 API 서버 대상 주기적 헬스체크(Health Check) 가벼운 배치 프로세스 구동.
* [Week 7-8] Kafka 이벤트 스트리밍 전환:
    - 로컬 Docker 환경에 단일 노드 Apache Kafka 브로커 구동 및 api-usage-logs topic 토폴로지 설계.
    - 게이트웨이 필터단에서 DB 직접 쓰기 로직 제거 -> API 호출 성공 즉시 Kafka 토픽으로 프로듀싱(Produce) 후 논블로킹 응답 반환.
    - 파이썬 워커 또는 자바 컨슈머가 Kafka 토픽의 이벤트를 받아 PostgreSQL billing_logs에 벌크(Bulk) 로드하고 유료 잔여 쿼터를 차감 정산하는 비동기 워커 시스템 완성.

### ☁️ Phase 3: 클라우드 네이티브 인프라 이주 (3개월 차)
* [Week 9-10] 컨테이너라이징 및 k8s 오케스트레이션 배포:
    - apiverse-gateway (Java) 및 apiverse-py-worker (Python) 애플리케이션을 가벼운 멀티 스테이지 Dockerfile 기반으로 이미지 래핑 및 로컬 레지스트리 푸시.
    - 홈서버 환경에 경량 k8s 클러스터(k3s) 구축 및 각 서비스별 Deployment, Service, Nginx Ingress Controller 라우팅 인프라 배포.
* [Week 11-12] 부하 스트레스 테스트 및 자동 확장 검증:
    - 부하 테스트 툴(JMeter 또는 nGrinder)을 세팅하고 가상의 유저 5,000명이 동시에 API 토큰 버킷 한도까지 찌르는 트래픽 폭주 시나리오 구동.
    - 가용성 트래픽 임계치 도달 시 k8s의 HPA(Horizontal Pod Autoscaler)가 자원(CPU/메모리) 부하를 감지해 게이트웨이 파드(Pod) 개수를 동적으로 자동 증설(Scale-out)하는지 모니터링 후 성능 개선 지표 수치화하여 포트폴리오 최종 문서화.
