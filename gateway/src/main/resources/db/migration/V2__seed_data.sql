-- ============================================================
-- api_products
-- ============================================================
INSERT INTO api_products (name, description, base_url, is_premium, is_active, category, calls_per_sec, spec_json)
VALUES (
  '기상청 날씨 API',
  '실시간 기온, 강수량, 바람, 습도 등 전국 기상 관측 데이터를 제공하는 공공 API입니다.',
  'https://api.weather.go.kr/v1',
  false, true, 'Weather', 5,
  '{"openapi":"3.0.0","info":{"title":"기상청 날씨 API","version":"1.0.0","description":"실시간 기상 데이터 조회"},"servers":[{"url":"https://api.weather.go.kr/v1"}],"paths":{"/current":{"get":{"summary":"현재 날씨 조회","parameters":[{"name":"region","in":"query","required":true,"schema":{"type":"string"},"description":"지역명 (예: 서울)"}],"responses":{"200":{"description":"성공","content":{"application/json":{"example":{"region":"서울","temp":22.4,"humidity":65,"wind":3.2,"condition":"맑음"}}}}},"security":[{"ApiKeyAuth":[]}]}},"/forecast":{"get":{"summary":"7일 날씨 예보","parameters":[{"name":"region","in":"query","required":true,"schema":{"type":"string"}}],"responses":{"200":{"description":"성공"}},"security":[{"ApiKeyAuth":[]}]}}},"components":{"securitySchemes":{"ApiKeyAuth":{"type":"apiKey","in":"header","name":"X-API-KEY"}}}}'
);

INSERT INTO api_products (name, description, base_url, is_premium, is_active, category, calls_per_sec, spec_json)
VALUES (
  '공공 주소 검색 API',
  '도로명 주소, 지번 주소, 우편번호 등 전국 주소 데이터를 검색할 수 있는 API입니다.',
  'https://api.juso.go.kr/v2',
  false, true, 'Location', 10,
  '{"openapi":"3.0.0","info":{"title":"공공 주소 검색 API","version":"2.0.0"},"servers":[{"url":"https://api.juso.go.kr/v2"}],"paths":{"/search":{"get":{"summary":"주소 검색","parameters":[{"name":"keyword","in":"query","required":true,"schema":{"type":"string"}},{"name":"page","in":"query","schema":{"type":"integer","default":1}},{"name":"size","in":"query","schema":{"type":"integer","default":10}}],"responses":{"200":{"description":"성공"}},"security":[{"ApiKeyAuth":[]}]}}},"components":{"securitySchemes":{"ApiKeyAuth":{"type":"apiKey","in":"header","name":"X-API-KEY"}}}}'
);

INSERT INTO api_products (name, description, base_url, is_premium, is_active, category, calls_per_sec, spec_json)
VALUES (
  '한국관광공사 관광정보 API',
  '전국 관광지, 숙박, 음식점, 축제 등 관광 정보를 제공하는 공공 API입니다.',
  'https://api.visitkorea.or.kr/v1',
  false, true, 'Tourism', 5,
  '{"openapi":"3.0.0","info":{"title":"한국관광공사 관광정보 API","version":"1.0.0"},"servers":[{"url":"https://api.visitkorea.or.kr/v1"}],"paths":{"/attractions":{"get":{"summary":"관광지 목록 조회","parameters":[{"name":"area","in":"query","schema":{"type":"string"}},{"name":"category","in":"query","schema":{"type":"string","enum":["관광지","숙박","음식점","축제"]}}],"responses":{"200":{"description":"성공"}},"security":[{"ApiKeyAuth":[]}]}}},"components":{"securitySchemes":{"ApiKeyAuth":{"type":"apiKey","in":"header","name":"X-API-KEY"}}}}'
);

INSERT INTO api_products (name, description, base_url, is_premium, is_active, category, calls_per_sec, spec_json)
VALUES (
  '실시간 주식 시세 API',
  '한국거래소(KRX) 실시간 주가, 거래량, 시가총액 데이터를 제공합니다. Pro 플랜 이상 사용 가능.',
  'https://api.krx-data.co.kr/v1',
  true, true, 'Finance', 20,
  '{"openapi":"3.0.0","info":{"title":"실시간 주식 시세 API","version":"1.0.0"},"servers":[{"url":"https://api.krx-data.co.kr/v1"}],"paths":{"/quote/{ticker}":{"get":{"summary":"종목 현재가 조회","parameters":[{"name":"ticker","in":"path","required":true,"schema":{"type":"string"},"description":"종목 코드 (예: 005930)"}],"responses":{"200":{"description":"성공"}},"security":[{"ApiKeyAuth":[]}]}}},"components":{"securitySchemes":{"ApiKeyAuth":{"type":"apiKey","in":"header","name":"X-API-KEY"}}}}'
);

-- ============================================================
-- users  (dev seed — password: "hypepia123")
-- ============================================================
INSERT INTO users (email, password_hash, company_name, tier, created_at)
VALUES (
  'dev@hypepia.com',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lH51',
  'Hypepia Inc.',
  'FREE',
  '2026-06-01 00:00:00'
);

-- ============================================================
-- api_keys
-- ============================================================
INSERT INTO api_keys (user_id, api_product_id, api_key_value, monthly_quota, used_quota, is_active, created_at)
SELECT u.id, p.id, 'apiverse_sandbox_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6', -1, 127, true, '2026-06-01 09:00:00'
FROM users u JOIN api_products p ON p.name = '기상청 날씨 API'
WHERE u.email = 'dev@hypepia.com';

INSERT INTO api_keys (user_id, api_product_id, api_key_value, monthly_quota, used_quota, is_active, created_at)
SELECT u.id, p.id, 'apiverse_sandbox_z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4', -1, 43, true, '2026-06-10 14:30:00'
FROM users u JOIN api_products p ON p.name = '공공 주소 검색 API'
WHERE u.email = 'dev@hypepia.com';

-- ============================================================
-- billing_logs  (mockUsageData 날짜별 건수 재현)
-- ============================================================

-- 2026-06-24: requests=45, errors=2
INSERT INTO billing_logs (api_key_value, request_path, http_method, response_status, client_ip, request_time)
SELECT
    CASE WHEN n % 3 = 0 THEN 'apiverse_sandbox_z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4'
         ELSE 'apiverse_sandbox_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6' END,
    CASE WHEN n % 5 = 0 THEN '/forecast' ELSE '/current' END,
    'GET',
    CASE WHEN n < 2 THEN 500 ELSE 200 END,
    '127.0.0.1',
    '2026-06-24 09:00:00'::timestamp + (n * interval '18 minutes')
FROM generate_series(0, 44) n;

-- 2026-06-25: requests=92, errors=5
INSERT INTO billing_logs (api_key_value, request_path, http_method, response_status, client_ip, request_time)
SELECT
    CASE WHEN n % 3 = 0 THEN 'apiverse_sandbox_z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4'
         ELSE 'apiverse_sandbox_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6' END,
    CASE WHEN n % 5 = 0 THEN '/forecast' ELSE '/current' END,
    'GET',
    CASE WHEN n < 5 THEN 500 ELSE 200 END,
    '127.0.0.1',
    '2026-06-25 09:00:00'::timestamp + (n * interval '6 minutes')
FROM generate_series(0, 91) n;

-- 2026-06-26: requests=78, errors=1
INSERT INTO billing_logs (api_key_value, request_path, http_method, response_status, client_ip, request_time)
SELECT
    CASE WHEN n % 3 = 0 THEN 'apiverse_sandbox_z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4'
         ELSE 'apiverse_sandbox_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6' END,
    CASE WHEN n % 5 = 0 THEN '/forecast' ELSE '/current' END,
    'GET',
    CASE WHEN n < 1 THEN 500 ELSE 200 END,
    '127.0.0.1',
    '2026-06-26 09:00:00'::timestamp + (n * interval '7 minutes')
FROM generate_series(0, 77) n;

-- 2026-06-27: requests=134, errors=8
INSERT INTO billing_logs (api_key_value, request_path, http_method, response_status, client_ip, request_time)
SELECT
    CASE WHEN n % 3 = 0 THEN 'apiverse_sandbox_z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4'
         ELSE 'apiverse_sandbox_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6' END,
    CASE WHEN n % 5 = 0 THEN '/forecast' ELSE '/current' END,
    'GET',
    CASE WHEN n < 8 THEN 500 ELSE 200 END,
    '127.0.0.1',
    '2026-06-27 09:00:00'::timestamp + (n * interval '4 minutes')
FROM generate_series(0, 133) n;

-- 2026-06-28: requests=61, errors=0
INSERT INTO billing_logs (api_key_value, request_path, http_method, response_status, client_ip, request_time)
SELECT
    CASE WHEN n % 3 = 0 THEN 'apiverse_sandbox_z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4'
         ELSE 'apiverse_sandbox_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6' END,
    CASE WHEN n % 5 = 0 THEN '/forecast' ELSE '/current' END,
    'GET', 200, '127.0.0.1',
    '2026-06-28 09:00:00'::timestamp + (n * interval '9 minutes')
FROM generate_series(0, 60) n;

-- 2026-06-29: requests=110, errors=3
INSERT INTO billing_logs (api_key_value, request_path, http_method, response_status, client_ip, request_time)
SELECT
    CASE WHEN n % 3 = 0 THEN 'apiverse_sandbox_z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4'
         ELSE 'apiverse_sandbox_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6' END,
    CASE WHEN n % 5 = 0 THEN '/forecast' ELSE '/current' END,
    'GET',
    CASE WHEN n < 3 THEN 500 ELSE 200 END,
    '127.0.0.1',
    '2026-06-29 09:00:00'::timestamp + (n * interval '5 minutes')
FROM generate_series(0, 109) n;

-- 2026-06-30: requests=88, errors=2
INSERT INTO billing_logs (api_key_value, request_path, http_method, response_status, client_ip, request_time)
SELECT
    CASE WHEN n % 3 = 0 THEN 'apiverse_sandbox_z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4'
         ELSE 'apiverse_sandbox_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6' END,
    CASE WHEN n % 5 = 0 THEN '/forecast' ELSE '/current' END,
    'GET',
    CASE WHEN n < 2 THEN 500 ELSE 200 END,
    '127.0.0.1',
    '2026-06-30 09:00:00'::timestamp + (n * interval '6 minutes')
FROM generate_series(0, 87) n;
