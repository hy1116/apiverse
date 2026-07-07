CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    company_name VARCHAR(100),
    phone       VARCHAR(20),
    tier        VARCHAR(20) DEFAULT 'FREE',
    role        VARCHAR(20) DEFAULT 'USER',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS api_products (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL UNIQUE,
    description  TEXT,
    base_url     VARCHAR(255) NOT NULL,
    is_premium   BOOLEAN DEFAULT FALSE,
    is_active    BOOLEAN DEFAULT TRUE,
    category     VARCHAR(50),
    calls_per_sec INT DEFAULT 5,
    response_type VARCHAR(20) DEFAULT 'JSON',
    spec_json    TEXT
);

CREATE TABLE IF NOT EXISTS api_keys (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    api_product_id  BIGINT REFERENCES api_products(id),
    api_key_value   VARCHAR(255) NOT NULL UNIQUE,
    white_list_ip   VARCHAR(255),
    monthly_quota   INT NOT NULL DEFAULT -1,
    used_quota      INT DEFAULT 0,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_api_key_value ON api_keys(api_key_value);

-- 기존 테이블에 누락된 컬럼 추가 (이미 있으면 무시)
ALTER TABLE users        ADD COLUMN IF NOT EXISTS phone         VARCHAR(20);
ALTER TABLE users        ADD COLUMN IF NOT EXISTS role          VARCHAR(20) DEFAULT 'USER';
ALTER TABLE users        ADD COLUMN IF NOT EXISTS updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE api_products ADD COLUMN IF NOT EXISTS category      VARCHAR(50);
ALTER TABLE api_products ADD COLUMN IF NOT EXISTS calls_per_sec INT DEFAULT 5;
ALTER TABLE api_products ADD COLUMN IF NOT EXISTS spec_json     TEXT;
ALTER TABLE api_products ADD COLUMN IF NOT EXISTS code               VARCHAR(120);
ALTER TABLE api_products ADD COLUMN IF NOT EXISTS upstream_api_key   VARCHAR(255);
ALTER TABLE api_products ADD COLUMN IF NOT EXISTS upstream_key_param VARCHAR(100);
ALTER TABLE api_products ADD COLUMN IF NOT EXISTS response_type      VARCHAR(20) DEFAULT 'JSON';
ALTER TABLE api_products ADD COLUMN IF NOT EXISTS created_by          BIGINT REFERENCES users(id);

-- response_type 컬럼 추가 이전 데이터 백필
UPDATE api_products SET response_type = 'JSON' WHERE response_type IS NULL;

-- 콤마로 여러 IP를 등록할 수 있도록 확장 (기존 설치본 대비)
ALTER TABLE api_keys ALTER COLUMN white_list_ip TYPE VARCHAR(255);

-- role 컬럼 분리 이전 데이터 마이그레이션: tier='ADMIN'이던 계정을 role='ADMIN'/tier='FREE'로 이전
UPDATE users SET role = 'ADMIN', tier = 'FREE' WHERE tier = 'ADMIN';

-- code 컬럼 추가 이전 데이터 백필 (product_id 기반 URL을 쓰던 기존 행들)
UPDATE api_products SET code = 'product-' || id WHERE code IS NULL;

-- api_products.name / code 유니크 인덱스 (ON CONFLICT (name) 에 필요)
CREATE UNIQUE INDEX IF NOT EXISTS api_products_name_unique ON api_products (name);
CREATE UNIQUE INDEX IF NOT EXISTS api_products_code_unique ON api_products (code);

CREATE TABLE IF NOT EXISTS inquiries (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    title       VARCHAR(200) NOT NULL,
    content     TEXT NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    answer      TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    answered_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS billing_logs (
    id              BIGSERIAL PRIMARY KEY,
    api_key_value   VARCHAR(255) NOT NULL,
    request_path    VARCHAR(255) NOT NULL,
    http_method     VARCHAR(10) NOT NULL,
    response_status INT NOT NULL,
    client_ip       VARCHAR(50),
    request_time    TIMESTAMP NOT NULL
);

-- /gateway/** 전역 접근 차단 IP 목록 (API 키/상품과 무관하게 ProxyService 최우선 검사)
CREATE TABLE IF NOT EXISTS blocked_ips (
    id          BIGSERIAL PRIMARY KEY,
    ip_address  VARCHAR(50) NOT NULL UNIQUE,
    reason      VARCHAR(255),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- gateway 앱 REST API(auth/product/key/inquiry 등) 전체 접근 로그.
-- /gateway/** 프록시 호출은 billing_logs에 api_key_value 기준으로 별도 기록되므로 여기서는 제외한다.
CREATE TABLE IF NOT EXISTS access_logs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    request_path    VARCHAR(255) NOT NULL,
    http_method     VARCHAR(10) NOT NULL,
    response_status INT NOT NULL,
    client_ip       VARCHAR(50),
    request_time    TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_access_logs_request_time ON access_logs(request_time);
