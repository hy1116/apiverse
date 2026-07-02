CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    company_name VARCHAR(100),
    phone       VARCHAR(20),
    tier        VARCHAR(20) DEFAULT 'FREE',
    role        VARCHAR(20) DEFAULT 'USER',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
    spec_json    TEXT
);

CREATE TABLE IF NOT EXISTS api_keys (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id),
    api_product_id  BIGINT REFERENCES api_products(id),
    api_key_value   VARCHAR(255) NOT NULL UNIQUE,
    white_list_ip   VARCHAR(50),
    monthly_quota   INT NOT NULL DEFAULT -1,
    used_quota      INT DEFAULT 0,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_api_key_value ON api_keys(api_key_value);

-- 기존 테이블에 누락된 컬럼 추가 (이미 있으면 무시)
ALTER TABLE users        ADD COLUMN IF NOT EXISTS phone         VARCHAR(20);
ALTER TABLE users        ADD COLUMN IF NOT EXISTS role          VARCHAR(20) DEFAULT 'USER';
ALTER TABLE api_products ADD COLUMN IF NOT EXISTS category      VARCHAR(50);
ALTER TABLE api_products ADD COLUMN IF NOT EXISTS calls_per_sec INT DEFAULT 5;
ALTER TABLE api_products ADD COLUMN IF NOT EXISTS spec_json     TEXT;

-- role 컬럼 분리 이전 데이터 마이그레이션: tier='ADMIN'이던 계정을 role='ADMIN'/tier='FREE'로 이전
UPDATE users SET role = 'ADMIN', tier = 'FREE' WHERE tier = 'ADMIN';

-- api_products.name 유니크 인덱스 (ON CONFLICT (name) 에 필요)
CREATE UNIQUE INDEX IF NOT EXISTS api_products_name_unique ON api_products (name);

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
