CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    company_name  VARCHAR(100),
    phone         VARCHAR(20),
    tier          VARCHAR(20) DEFAULT 'FREE',
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE api_products (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL UNIQUE,
    description   TEXT,
    base_url      VARCHAR(255) NOT NULL,
    is_premium    BOOLEAN DEFAULT FALSE,
    is_active     BOOLEAN DEFAULT TRUE,
    category      VARCHAR(50),
    calls_per_sec INT DEFAULT 5,
    spec_json     TEXT
);

CREATE TABLE api_keys (
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

CREATE INDEX idx_api_key_value ON api_keys(api_key_value);

CREATE TABLE billing_logs (
    id              BIGSERIAL PRIMARY KEY,
    api_key_value   VARCHAR(255) NOT NULL,
    request_path    VARCHAR(255) NOT NULL,
    http_method     VARCHAR(10) NOT NULL,
    response_status INT NOT NULL,
    client_ip       VARCHAR(50),
    request_time    TIMESTAMP NOT NULL
);
