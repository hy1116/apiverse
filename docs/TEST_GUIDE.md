# APIverse 테스트 가이드

## 사전 준비

### 서버 실행
```bash
# PostgreSQL 실행 (Docker)
docker-compose up -d

# 백엔드 실행
gradlew.bat :gateway:bootRun

# 프론트엔드 실행 (별도 터미널)
cd front && npm run dev
```

- 백엔드: `http://localhost:8080`
- 프론트엔드: `http://localhost:5173`

### 개발용 시드 계정
| 항목 | 값 |
|---|---|
| 이메일 | `dev@hypepia.com` |
| 비밀번호 | `hypepia123` |
| 플랜 | FREE |

---

## 1. 인증 API

### 회원가입
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "companyName": "테스트 회사",
    "phone": "010-1234-5678"
  }'
```

**응답:**
```json
{
  "id": 2,
  "email": "test@example.com",
  "companyName": "테스트 회사",
  "tier": "FREE",
  "token": "eyJhbGci..."
}
```

### 로그인
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "dev@hypepia.com", "password": "hypepia123"}'
```

> 이후 모든 요청에서 응답의 `token` 값을 `Authorization: Bearer {token}` 헤더에 사용합니다.

### 이메일 중복 확인
```bash
curl "http://localhost:8080/api/auth/check-email?email=dev@hypepia.com"
# {"available": false}

curl "http://localhost:8080/api/auth/check-email?email=new@example.com"
# {"available": true}
```

---

## 2. API 상품 조회

### 전체 목록 (인증 불필요)
```bash
curl http://localhost:8080/api/products
```

### 단건 조회
```bash
curl http://localhost:8080/api/products/1
```

### 내 키 조회 (로그인 시 해당 키 반환, 비로그인 시 `{}`)
```bash
curl http://localhost:8080/api/products/1/my-key \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

---

## 3. API 키 관리

> 모든 요청에 `Authorization: Bearer {JWT_TOKEN}` 헤더 필요

### 내 키 목록 조회
```bash
curl http://localhost:8080/api/keys \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

**응답 예시:**
```json
[
  {
    "id": 1,
    "apiKeyValue": "apiverse_sandbox_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
    "apiProductId": 1,
    "apiProductName": "기상청 날씨 API",
    "monthlyQuota": -1,
    "usedQuota": 127,
    "isActive": true
  }
]
```

### API 키 발급
```bash
curl -X POST http://localhost:8080/api/keys \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"apiProductId": 1}'
```

- 동일 상품에 키가 이미 있으면 `409 Conflict` 반환
- 키 형식: `apiverse_sandbox_{32자리 UUID (하이픈 제거)}`

### API 키 폐기
```bash
curl -X DELETE http://localhost:8080/api/keys/1 \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

- 본인 키가 아닌 경우 `403 Forbidden`
- 존재하지 않는 경우 `404 Not Found`

---

## 4. 사용량 조회

### 7일 일별 통계 (인증 불필요)
```bash
curl http://localhost:8080/api/usage/daily
```

**응답 예시:**
```json
[
  {"date": "06/24", "requests": 45, "errors": 2},
  {"date": "06/25", "requests": 92, "errors": 5}
]
```

---

## 5. 발급받은 키로 외부 API 호출

발급된 `apiverse_sandbox_xxx` 키는 외부 API 호출 시 `X-API-KEY` 헤더에 사용합니다.

### 기상청 날씨 API
```bash
# 현재 날씨
curl "https://api.weather.go.kr/v1/current?region=서울" \
  -H "X-API-KEY: apiverse_sandbox_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"

# 7일 예보
curl "https://api.weather.go.kr/v1/forecast?region=서울" \
  -H "X-API-KEY: apiverse_sandbox_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"
```

### 공공 주소 검색 API
```bash
curl "https://api.juso.go.kr/v2/search?keyword=강남구&page=1&size=10" \
  -H "X-API-KEY: apiverse_sandbox_z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4"
```

### 한국관광공사 관광정보 API
```bash
curl "https://api.visitkorea.or.kr/v1/attractions?area=서울&category=관광지" \
  -H "X-API-KEY: {YOUR_SANDBOX_KEY}"
```

### 실시간 주식 시세 API (Pro 플랜 전용)
```bash
curl "https://api.krx-data.co.kr/v1/quote/005930" \
  -H "X-API-KEY: {YOUR_SANDBOX_KEY}"
```

---

## 6. 프론트엔드 테스트 시나리오

### 신규 사용자 플로우
1. `http://localhost:5173/signup` 접속 → 회원가입
2. `/dashboard` 에서 통계 및 키 확인
3. `무료 API 1-Click 연동` 섹션에서 **즉시 발급** 또는 **상세보기** 클릭
4. `/marketplace/{id}` 상세 페이지에서 Swagger UI / 코드 스니펫 확인

### 대시보드 → API 상세 이동 방법
- **내 Sandbox API Keys** 카드: 상품명 클릭 또는 **상세보기** 버튼
- **무료 API 1-Click 연동** 카드: 상품명 클릭 또는 **상세보기** 버튼

### Swagger UI 테스트
1. `/marketplace/{id}` 접속
2. **Swagger UI** 탭에서 API 스펙 확인
3. **코드 스니펫** 탭에서 언어별 예제 코드 확인 (키 발급 시 실제 키 자동 삽입)

---

## 7. 에러 케이스

| 상황 | 상태코드 | 메시지 |
|---|---|---|
| 토큰 없이 인증 필요 엔드포인트 요청 | `401` | Unauthorized |
| 이미 발급된 키 재발급 시도 | `409` | 이미 발급된 키가 있습니다. |
| 다른 사용자의 키 폐기 시도 | `403` | Forbidden |
| 존재하지 않는 상품/키 조회 | `404` | Not Found |
| 이미 사용 중인 이메일로 가입 | `409` | 이미 사용 중인 이메일입니다. |
| 잘못된 비밀번호로 로그인 | `401` | 이메일 또는 비밀번호가 올바르지 않습니다. |
