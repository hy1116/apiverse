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
) ON CONFLICT (name) DO NOTHING;

INSERT INTO api_products (name, description, base_url, is_premium, is_active, category, calls_per_sec, spec_json)
VALUES (
  '공공 주소 검색 API',
  '도로명 주소, 지번 주소, 우편번호 등 전국 주소 데이터를 검색할 수 있는 API입니다.',
  'https://api.juso.go.kr/v2',
  false, true, 'Location', 10,
  '{"openapi":"3.0.0","info":{"title":"공공 주소 검색 API","version":"2.0.0"},"servers":[{"url":"https://api.juso.go.kr/v2"}],"paths":{"/search":{"get":{"summary":"주소 검색","parameters":[{"name":"keyword","in":"query","required":true,"schema":{"type":"string"}},{"name":"page","in":"query","schema":{"type":"integer","default":1}},{"name":"size","in":"query","schema":{"type":"integer","default":10}}],"responses":{"200":{"description":"성공"}},"security":[{"ApiKeyAuth":[]}]}}},"components":{"securitySchemes":{"ApiKeyAuth":{"type":"apiKey","in":"header","name":"X-API-KEY"}}}}'
) ON CONFLICT (name) DO NOTHING;

INSERT INTO api_products (name, description, base_url, is_premium, is_active, category, calls_per_sec, spec_json)
VALUES (
  '한국관광공사 관광정보 API',
  '전국 관광지, 숙박, 음식점, 축제 등 관광 정보를 제공하는 공공 API입니다.',
  'https://api.visitkorea.or.kr/v1',
  false, true, 'Tourism', 5,
  '{"openapi":"3.0.0","info":{"title":"한국관광공사 관광정보 API","version":"1.0.0"},"servers":[{"url":"https://api.visitkorea.or.kr/v1"}],"paths":{"/attractions":{"get":{"summary":"관광지 목록 조회","parameters":[{"name":"area","in":"query","schema":{"type":"string"}},{"name":"category","in":"query","schema":{"type":"string","enum":["관광지","숙박","음식점","축제"]}}],"responses":{"200":{"description":"성공"}},"security":[{"ApiKeyAuth":[]}]}}},"components":{"securitySchemes":{"ApiKeyAuth":{"type":"apiKey","in":"header","name":"X-API-KEY"}}}}'
) ON CONFLICT (name) DO NOTHING;

INSERT INTO api_products (name, description, base_url, is_premium, is_active, category, calls_per_sec, spec_json)
VALUES (
  '실시간 주식 시세 API',
  '한국거래소(KRX) 실시간 주가, 거래량, 시가총액 데이터를 제공합니다. Pro 플랜 이상 사용 가능.',
  'https://api.krx-data.co.kr/v1',
  true, true, 'Finance', 20,
  '{"openapi":"3.0.0","info":{"title":"실시간 주식 시세 API","version":"1.0.0"},"servers":[{"url":"https://api.krx-data.co.kr/v1"}],"paths":{"/quote/{ticker}":{"get":{"summary":"종목 현재가 조회","parameters":[{"name":"ticker","in":"path","required":true,"schema":{"type":"string"},"description":"종목 코드 (예: 005930)"}],"responses":{"200":{"description":"성공"}},"security":[{"ApiKeyAuth":[]}]}}},"components":{"securitySchemes":{"ApiKeyAuth":{"type":"apiKey","in":"header","name":"X-API-KEY"}}}}'
) ON CONFLICT (name) DO NOTHING;

-- ============================================================
-- users  (dev seed account — password: "hypepia123")
-- BCrypt(10) hash — 인증 구현 후 실제 가입 플로우로 대체
-- ============================================================
INSERT INTO users (email, password_hash, company_name, tier, role, created_at)
VALUES (
  'dev@hypepia.com',
  '$2a$10$D4ZntdHEJNHQLUhAmyMJKuCOS/1efCznwmdJG9FZtXTqmukYaoohe',
  'Hypepia Inc.',
  'FREE',
  'USER',
  '2026-06-01 00:00:00'
) ON CONFLICT (email) DO UPDATE SET password_hash = EXCLUDED.password_hash;

-- ============================================================
-- users  (admin seed account — password: "admin1234!")
-- BCrypt(10) hash — admin 콘솔(:8090) 로그인 테스트용
-- ============================================================
INSERT INTO users (email, password_hash, company_name, tier, role, created_at)
VALUES (
  'admin@apiverse.com',
  '$2a$10$c0Qa8IikffrdG21mkO7o4OCItiIZ/i.7AOGQdHYUv/YwirPwwhNNa',
  'ApiVerse',
  'FREE',
  'ADMIN',
  '2026-06-01 00:00:00'
) ON CONFLICT (email) DO UPDATE SET password_hash = EXCLUDED.password_hash;

-- ============================================================
-- api_keys  (mockApiKeys — dev 계정에 연결)
-- ============================================================
INSERT INTO api_keys (user_id, api_product_id, api_key_value, monthly_quota, used_quota, is_active, created_at)
SELECT u.id, p.id,
       'apiverse_sandbox_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6',
       -1, 127, true, '2026-06-01 09:00:00'
FROM users u
JOIN api_products p ON p.name = '기상청 날씨 API'
WHERE u.email = 'dev@hypepia.com'
ON CONFLICT (api_key_value) DO NOTHING;

INSERT INTO api_keys (user_id, api_product_id, api_key_value, monthly_quota, used_quota, is_active, created_at)
SELECT u.id, p.id,
       'apiverse_sandbox_z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4',
       -1, 43, true, '2026-06-10 14:30:00'
FROM users u
JOIN api_products p ON p.name = '공공 주소 검색 API'
WHERE u.email = 'dev@hypepia.com'
ON CONFLICT (api_key_value) DO NOTHING;

-- ============================================================
-- inquiries  (샘플 1:1 문의)
-- ============================================================
INSERT INTO inquiries (user_id, title, content, status, answer, created_at, answered_at)
SELECT
    (SELECT id FROM users WHERE email = 'dev@hypepia.com'),
    'PRO 플랜 업그레이드 후 한도 적용 시점 문의',
    'PRO 플랜으로 업그레이드하면 API 호출 한도가 언제부터 적용되나요? 즉시 적용인지 다음 달부터인지 궁금합니다.',
    'ANSWERED',
    '안녕하세요! 업그레이드 즉시 새로운 한도가 적용됩니다. 궁금한 점이 있으시면 언제든지 문의해 주세요.',
    NOW() - INTERVAL '3 days',
    NOW() - INTERVAL '2 days'
WHERE NOT EXISTS (
    SELECT 1 FROM inquiries WHERE title = 'PRO 플랜 업그레이드 후 한도 적용 시점 문의'
);

INSERT INTO inquiries (user_id, title, content, status, created_at)
SELECT
    (SELECT id FROM users WHERE email = 'dev@hypepia.com'),
    'Sandbox 키로 실제 API 호출이 가능한가요?',
    '발급받은 Sandbox 키를 사용해 외부 API를 직접 호출할 수 있는지 궁금합니다. 프록시 방식인지 아니면 키만 전달하는 방식인지도 알고 싶습니다.',
    'PENDING',
    NOW() - INTERVAL '1 day'
WHERE NOT EXISTS (
    SELECT 1 FROM inquiries WHERE title = 'Sandbox 키로 실제 API 호출이 가능한가요?'
);

-- ============================================================
-- billing_logs  (mockUsageData 날짜별 정확한 건수 재현)
-- NOT EXISTS 가드: 해당 날짜 데이터가 없을 때만 INSERT
-- ============================================================

-- D-6: requests=45, errors=2
INSERT INTO billing_logs (api_key_value, request_path, http_method, response_status, client_ip, request_time)
SELECT
    CASE WHEN n % 3 = 0 THEN 'apiverse_sandbox_z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4'
         ELSE 'apiverse_sandbox_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6' END,
    CASE WHEN n % 5 = 0 THEN '/forecast' ELSE '/current' END,
    'GET',
    CASE WHEN n < 2 THEN 500 ELSE 200 END,
    '127.0.0.1',
    (CURRENT_DATE - 6) + interval '9 hours' + (n * interval '18 minutes')
FROM generate_series(0, 44) n
WHERE NOT EXISTS (SELECT 1 FROM billing_logs WHERE request_time::date = CURRENT_DATE - 6);

-- D-5: requests=92, errors=5
INSERT INTO billing_logs (api_key_value, request_path, http_method, response_status, client_ip, request_time)
SELECT
    CASE WHEN n % 3 = 0 THEN 'apiverse_sandbox_z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4'
         ELSE 'apiverse_sandbox_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6' END,
    CASE WHEN n % 5 = 0 THEN '/forecast' ELSE '/current' END,
    'GET',
    CASE WHEN n < 5 THEN 500 ELSE 200 END,
    '127.0.0.1',
    (CURRENT_DATE - 5) + interval '9 hours' + (n * interval '6 minutes')
FROM generate_series(0, 91) n
WHERE NOT EXISTS (SELECT 1 FROM billing_logs WHERE request_time::date = CURRENT_DATE - 5);

-- D-4: requests=78, errors=1
INSERT INTO billing_logs (api_key_value, request_path, http_method, response_status, client_ip, request_time)
SELECT
    CASE WHEN n % 3 = 0 THEN 'apiverse_sandbox_z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4'
         ELSE 'apiverse_sandbox_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6' END,
    CASE WHEN n % 5 = 0 THEN '/forecast' ELSE '/current' END,
    'GET',
    CASE WHEN n < 1 THEN 500 ELSE 200 END,
    '127.0.0.1',
    (CURRENT_DATE - 4) + interval '9 hours' + (n * interval '7 minutes')
FROM generate_series(0, 77) n
WHERE NOT EXISTS (SELECT 1 FROM billing_logs WHERE request_time::date = CURRENT_DATE - 4);

-- D-3: requests=134, errors=8
INSERT INTO billing_logs (api_key_value, request_path, http_method, response_status, client_ip, request_time)
SELECT
    CASE WHEN n % 3 = 0 THEN 'apiverse_sandbox_z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4'
         ELSE 'apiverse_sandbox_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6' END,
    CASE WHEN n % 5 = 0 THEN '/forecast' ELSE '/current' END,
    'GET',
    CASE WHEN n < 8 THEN 500 ELSE 200 END,
    '127.0.0.1',
    (CURRENT_DATE - 3) + interval '9 hours' + (n * interval '4 minutes')
FROM generate_series(0, 133) n
WHERE NOT EXISTS (SELECT 1 FROM billing_logs WHERE request_time::date = CURRENT_DATE - 3);

-- D-2: requests=61, errors=0
INSERT INTO billing_logs (api_key_value, request_path, http_method, response_status, client_ip, request_time)
SELECT
    CASE WHEN n % 3 = 0 THEN 'apiverse_sandbox_z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4'
         ELSE 'apiverse_sandbox_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6' END,
    CASE WHEN n % 5 = 0 THEN '/forecast' ELSE '/current' END,
    'GET',
    200,
    '127.0.0.1',
    (CURRENT_DATE - 2) + interval '9 hours' + (n * interval '9 minutes')
FROM generate_series(0, 60) n
WHERE NOT EXISTS (SELECT 1 FROM billing_logs WHERE request_time::date = CURRENT_DATE - 2);

-- D-1: requests=110, errors=3
INSERT INTO billing_logs (api_key_value, request_path, http_method, response_status, client_ip, request_time)
SELECT
    CASE WHEN n % 3 = 0 THEN 'apiverse_sandbox_z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4'
         ELSE 'apiverse_sandbox_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6' END,
    CASE WHEN n % 5 = 0 THEN '/forecast' ELSE '/current' END,
    'GET',
    CASE WHEN n < 3 THEN 500 ELSE 200 END,
    '127.0.0.1',
    (CURRENT_DATE - 1) + interval '9 hours' + (n * interval '5 minutes')
FROM generate_series(0, 109) n
WHERE NOT EXISTS (SELECT 1 FROM billing_logs WHERE request_time::date = CURRENT_DATE - 1);

-- D-0 (오늘): requests=88, errors=2
INSERT INTO billing_logs (api_key_value, request_path, http_method, response_status, client_ip, request_time)
SELECT
    CASE WHEN n % 3 = 0 THEN 'apiverse_sandbox_z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4'
         ELSE 'apiverse_sandbox_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6' END,
    CASE WHEN n % 5 = 0 THEN '/forecast' ELSE '/current' END,
    'GET',
    CASE WHEN n < 2 THEN 500 ELSE 200 END,
    '127.0.0.1',
    CURRENT_DATE + interval '9 hours' + (n * interval '6 minutes')
FROM generate_series(0, 87) n
WHERE NOT EXISTS (SELECT 1 FROM billing_logs WHERE request_time::date = CURRENT_DATE);
