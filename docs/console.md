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
│   ├── Navbar.jsx        대시보드(고정) + 그룹 드롭다운 3개(NAV_GROUPS, 하단 "메뉴 그룹" 참고)
│   ├── ProtectedRoute.jsx    미인증 → /login
│   └── ErrorBoundary.jsx     런타임 에러 화면
└── pages/                LoginPage, DashboardPage,
                          ProductsPage, ProductDetailPage,
                          InquiriesPage,
                          UsersPage, UserDetailPage,
                          ApiKeysPage, ApiKeyDetailPage,
                          BillingLogsPage, StatsPage, BlockedIpsPage,
                          NotFoundPage
```

`web`과 달리 회원가입 페이지가 없다 (관리자 계정은 DB에서 직접 role='ADMIN'으로 생성).

목록 페이지는 조회만 남기고, 승인/반려·tier 변경·키 폐기·code/업스트림 키 편집처럼 민감하거나 상세 컨텍스트가 필요한 조작은 전부 상세 페이지(행 클릭 → `/{리소스}/:id`)로 옮겨져 있다.

## 메뉴 그룹 (Navbar)

`대시보드`는 그룹에 속하지 않은 고정 링크. 나머지는 역할별로 3개 드롭다운 그룹으로 묶여 있다 (`Navbar.jsx`의 `NAV_GROUPS`):

| 그룹 | 하위 메뉴 |
|---|---|
| API 관리 (서비스 운영) | API 상품, API 키, 요청 로그 |
| 회원/서비스 관리 (사용자/문의) | 회원, 문의 |
| 시스템/보안 (인프라 관리) | 차단 IP 관리, 통계 |

그룹 버튼은 하위 경로 중 하나라도 현재 경로와 일치하면 활성 스타일로 표시된다 (`group.links.some(l => location.pathname.startsWith(l.to))`).

## 라우트

| 경로 | 컴포넌트 | 접근 |
|---|---|---|
| `/login` | LoginPage | 공개 (로그인 시 /dashboard 리다이렉트) |
| `/dashboard` | DashboardPage | ProtectedRoute — 승인 대기 상품 수 + 7일 요청/에러 통계 |
| `/products` | ProductsPage | ProtectedRoute — 전체 상품 목록(읽기 전용), 행 클릭 시 상세로 |
| `/products/:id` | ProductDetailPage | ProtectedRoute — code/설명/baseUrl/카테고리/호출제한/프리미엄/응답타입/업스트림 API 키 편집, 승인/반려는 대기중 상품에 한해 여기서만 |
| `/inquiries` | InquiriesPage | ProtectedRoute — 전체 문의 목록, 답변 등록 |
| `/users` | UsersPage | ProtectedRoute — 전체 회원 목록(읽기 전용), 행 클릭 시 상세로 |
| `/users/:id` | UserDetailPage | ProtectedRoute — 회원 상세, tier 변경은 여기서만 |
| `/keys` | ApiKeysPage | ProtectedRoute — 전체 API 키 목록(읽기 전용), 행 클릭 시 상세로 |
| `/keys/:id` | ApiKeyDetailPage | ProtectedRoute — 키 상세, 폐기·허용 IP(화이트리스트)·월간 쿼터 설정은 여기서만 |
| `/logs` | BillingLogsPage | ProtectedRoute — `billing_logs` 원본 목록, 전체/에러만(5xx) 필터 + 조회 기간(오늘/7일/30일/90일) + 페이지네이션(50건/페이지) |
| `/stats` | StatsPage | ProtectedRoute — 에러/사용률 탭(기간: 오늘/7일/30일). 에러: 상품별 에러율 랭킹·에러 많은 API 키 Top20·상태코드 분포. 사용률: 상품별 사용량 랭킹·호출량 많은 API 키 Top20·쿼터 사용률 Top20 |
| `/blocked-ips` | BlockedIpsPage | ProtectedRoute — 전역 차단 IP 추가/조회/해제 |
| `*` | NotFoundPage | 공개 (404) |

## 인증

- `localStorage('av_admin_user')`: `{ id, email, companyName, tier, role, token }` 저장. `web`의 `av_user`와 키를 다르게 써서 같은 브라우저에서 두 앱을 오가도 세션이 섞이지 않는다.
- `client.js` 요청 인터셉터: `Authorization: Bearer {token}` 자동 삽입
- `client.js` 응답 인터셉터: 401 → localStorage 초기화 + `/login` 리다이렉트 (단, `/admin/auth/*` 요청 자체의 401은 리다이렉트하지 않음 — 로그인 실패 메시지를 보여줘야 하므로)

## 백엔드 연동

모든 API 호출은 `admin` 모듈(`docs/admin.md`)의 `/api/admin/**` 엔드포인트를 사용한다. 차트 라이브러리(recharts)는 사용하지 않고 통계는 표 형태로만 표시 — 필요해지면 `web`의 DashboardPage 패턴을 참고해 추가.

## 개발 서버 실행

```bash
cd console && npm install && npm run dev   # localhost:3001, /api → :8090
```
