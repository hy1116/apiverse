# web 모듈

React 18 + Vite 5 + Tailwind CSS 3 SPA (사용자용, `www.apiverse.com`). Vite dev 서버가 `/api/*`를 `http://127.0.0.1:8080`으로 프록시 (IPv4 명시 — Node.js IPv6 오작동 방지).

## 파일 구조

```
web/src/
├── App.jsx              라우터 + ErrorBoundary
├── api/client.js        Axios 인스턴스 (baseURL: /api, 인터셉터 포함)
├── context/
│   ├── AuthContext.jsx  인증 상태, localStorage('av_user') 동기화
│   └── ThemeContext.jsx html.dark 클래스 토글
├── components/
│   ├── Navbar.jsx
│   ├── ProtectedRoute.jsx   미인증 → /login
│   └── ErrorBoundary.jsx    런타임 에러 → 500 화면
└── pages/               LandingPage, LoginPage, SignupPage, DashboardPage,
                         MarketplacePage, ApiDetailPage, RegisterApiPage, MyProductsPage,
                         ProfilePage, LogsPage, InquiryPage, NotFoundPage
```

## 라우트

| 경로 | 컴포넌트 | 접근 |
|---|---|---|
| `/` | LandingPage | 공개 (로그인 시 /dashboard 리다이렉트) |
| `/login`, `/signup` | - | 공개 |
| `/dashboard` | DashboardPage | ProtectedRoute |
| `/marketplace` | MarketplacePage | ProtectedRoute |
| `/marketplace/register` | RegisterApiPage | ProtectedRoute |
| `/marketplace/my` | MyProductsPage | ProtectedRoute — 내가 등록한 상품 목록 + 승인 상태(대기중/승인됨) |
| `/marketplace/:id` | ApiDetailPage | 공개 |
| `/profile` | ProfilePage | ProtectedRoute — 내 정보(회사명/전화번호) 수정, 이메일은 읽기 전용 |
| `/logs` | LogsPage | ProtectedRoute — 내 API 키의 요청 로그, API별 필터 + 전체/에러만 + 조회 기간(오늘/7일/30일/90일) + 페이지네이션(50건/페이지) |
| `/inquiry` | InquiryPage | ProtectedRoute |
| `*` | NotFoundPage | 공개 (404) |

## 인증

- `localStorage('av_user')`: `{ id, email, companyName, tier, token }` 저장
- `client.js` 요청 인터셉터: `Authorization: Bearer {token}` 자동 삽입
- `client.js` 응답 인터셉터: 401 → localStorage 초기화 + `/login` 리다이렉트
- `AuthContext.updateUser(partial)`: 로그인 상태 유지한 채 `av_user`의 일부 필드만 갱신 (재로그인 없이 프로필 수정 반영). `ProfilePage`가 저장 성공 시 `updateUser({ companyName })`으로 Navbar 등에 표시되는 회사명을 즉시 동기화한다.

## 비직관적인 사항

- **다크 모드**: `darkMode: 'class'` (Tailwind). SwaggerUI 다크 오버라이드는 `src/index.css`에 별도 작성.
- **RegisterApiPage**: 3단계 폼. 1단계(Swagger/OpenAPI 스펙 URL 입력)는 선택 사항 — "Swagger 없이 직접 입력하기" 버튼으로 스펙 없이 바로 2단계(상품 정보 입력: name/description/baseUrl/category/callsPerSec/responseType 수동 입력)로 건너뛸 수 있다. 이 경우 `spec` state가 `null`로 유지되고 `specJson`은 `null`로 전송된다 (백엔드 `api_products.spec_json`은 nullable). 제출 시 `POST /api/products` → `isActive=false` 저장 → 관리자 승인 대기. SuccessStep에 "관리자 승인 후 게시" 문구.
- **ApiDetailPage**: `spec_json`을 SwaggerUI로 렌더링. `@JsonRawValue` 덕분에 파싱 가능. `specJson`이 없는 상품(Swagger 없이 등록)은 "Swagger UI" 탭 자체가 숨겨지고 "코드 스니펫" 탭만 노출된다.
- **LandingPage**: 라이트 모드 배경 `bg-slate-50` (흰색 아님), 섹션 이미지는 `public/imgs/dash_board.png`.
- **ProfilePage**: `GET/PATCH /api/profile` 사용 (`/api/auth/me`가 아님 — `gateway.md`의 "Security 경로 규칙" 참고). PATCH는 `companyName`/`phone`이 `null`이면 무시, 빈 문자열이면 필드를 지운다.
- **LogsPage**: `GET /api/usage/logs`는 `apiKeyValue`만 반환하고 상품명은 안 주므로, 별도로 `GET /api/keys`를 불러와 `apiKeyValue → apiProductName` 매핑을 프론트에서 만들어 테이블에 표시한다. admin `console`의 `BillingLogsPage`와 UI 패턴은 같지만 이쪽은 항상 본인 소유 키의 로그만 보인다(`UsageController.logs()`가 principal의 userId로 서버에서 강제).
