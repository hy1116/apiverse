# TODO

## 기능 구멍 (Function Gaps)

- [x] `ProxyService` — 호출 성공 시 `used_quota` 증가 누락
- [x] `ProxyService` — `white_list_ip` 검증 로직 없음
- [x] `POST /api/products` 엔드포인트 없음 (ADMIN 전용 등록)
- [x] `RegisterApiPage` — `handleSubmit`이 `setTimeout` 가짜 구현, 실제 API 호출 없음

## 품질 / 안정성

- [x] 월별 쿼터 리셋 스케줄러 (`used_quota` 매월 1일 초기화)
- [x] 전역 에러 페이지 (404, 500)
- [x] Proxy, Inquiry 컨트롤러 테스트 미작성

## 부가 기능

- [ ] 관리자 화면 (문의 관리, API 상품 관리)
- [ ] 회원 프로필 수정 페이지
- [ ] Signup 백엔드 유효성 강화 (비밀번호 정책 등)

## 배포 준비

- [ ] 백엔드 Dockerfile
- [ ] 환경 변수 정리 문서화 (`.env.example`)
