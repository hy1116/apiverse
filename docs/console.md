# console 모듈

React 18 + Vite 5 + Tailwind CSS 3 어드민 SPA (`admin.apiverse.com`). Vite dev 서버가 `/api/*`를 `http://127.0.0.1:8090`(admin 모듈)으로 프록시. `web` 모듈과 구조/스타일 컨벤션을 그대로 따르되 인증 대상만 ADMIN tier로 제한.

## 파일 구조

```
console/src/
├── App.jsx              라우터 + ErrorBoundary
├── api/client.js         Axios 인스턴스 (baseURL: /api, 인터셉터 포함)
├── context/
│   ├── AuthContext.jsx   인증 상태, localStorage('av_admin_user') 동기화
│   └── ThemeContext.jsx  html.dark 클래스 토글 (web과 동일)
├── components/
│   ├── Navbar.jsx        대시보드/API 상품/문의/회원/API 키 링크
│   ├── ProtectedRoute.jsx    미인증 → /login
│   └── ErrorBoundary.jsx     런타임 에러 화면
└── pages/                LoginPage, DashboardPage, ProductsPage,
                          InquiriesPage, UsersPage, ApiKeysPage, NotFoundPage
```

`web`과 달리 회원가입 페이지가 없다 (관리자 계정은 DB에서 직접 tier='ADMIN'으로 생성).

## 라우트

| 경로 | 컴포넌트 | 접근 |
|---|---|---|
| `/login` | LoginPage | 공개 (로그인 시 /dashboard 리다이렉트) |
| `/dashboard` | DashboardPage | ProtectedRoute — 승인 대기 상품 수 + 7일 요청/에러 통계 |
| `/products` | ProductsPage | ProtectedRoute — 전체 상품 목록, 승인/반려 |
| `/inquiries` | InquiriesPage | ProtectedRoute — 전체 문의 목록, 답변 등록 |
| `/users` | UsersPage | ProtectedRoute — 전체 회원 목록, tier 변경 |
| `/keys` | ApiKeysPage | ProtectedRoute — 전체 API 키 목록, 강제 폐기 |
| `*` | NotFoundPage | 공개 (404) |

## 인증

- `localStorage('av_admin_user')`: `{ id, email, companyName, tier, token }` 저장. `web`의 `av_user`와 키를 다르게 써서 같은 브라우저에서 두 앱을 오가도 세션이 섞이지 않는다.
- `client.js` 요청 인터셉터: `Authorization: Bearer {token}` 자동 삽입
- `client.js` 응답 인터셉터: 401 → localStorage 초기화 + `/login` 리다이렉트 (단, `/admin/auth/*` 요청 자체의 401은 리다이렉트하지 않음 — 로그인 실패 메시지를 보여줘야 하므로)

## 백엔드 연동

모든 API 호출은 `admin` 모듈(`docs/admin.md`)의 `/api/admin/**` 엔드포인트를 사용한다. 차트 라이브러리(recharts)는 사용하지 않고 통계는 표 형태로만 표시 — 필요해지면 `web`의 DashboardPage 패턴을 참고해 추가.

## 개발 서버 실행

```bash
cd console && npm install && npm run dev   # localhost:3001, /api → :8090
```
