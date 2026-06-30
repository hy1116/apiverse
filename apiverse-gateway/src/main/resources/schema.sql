CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    company_name VARCHAR(100),
    tier        VARCHAR(20) DEFAULT 'FREE',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS api_products (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    base_url    VARCHAR(255) NOT NULL,
    is_premium  BOOLEAN DEFAULT FALSE,
    is_active   BOOLEAN DEFAULT TRUE
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

CREATE TABLE IF NOT EXISTS billing_logs (
    id              BIGSERIAL PRIMARY KEY,
    api_key_value   VARCHAR(255) NOT NULL,
    request_path    VARCHAR(255) NOT NULL,
    http_method     VARCHAR(10) NOT NULL,
    response_status INT NOT NULL,
    client_ip       VARCHAR(50),
    request_time    TIMESTAMP NOT NULL
);
