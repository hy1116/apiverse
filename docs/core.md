# core 모듈

Spring Boot 플러그인 없는 순수 Java 라이브러리. gateway가 의존하는 공유 인프라.

## 패키지 구조

```
core/
├── config/      R2dbcRepositoryConfig (@EnableR2dbcRepositories)
├── entity/      User, ApiProduct, ApiKey, BillingLog, Inquiry
├── projection/  DailyStat (SQL 쿼리 결과 매핑)
└── repository/  UserRepository, ApiProductRepository, ApiKeyRepository,
                 BillingLogRepository, InquiryRepository
```

## 엔티티 주의사항

- `ApiProduct.specJson` — `@JsonRawValue`: JSON 문자열을 이스케이프 없이 응답에 삽입. SwaggerUI 파싱에 필수.
- `ApiProduct`, `Inquiry` — `@Builder(toBuilder = true)`: 부분 업데이트(승인, 답변 저장)에 사용.
- `ApiKey.monthlyQuota` — `-1`은 무제한.
- `Inquiry.status` — `"PENDING"` | `"ANSWERED"` 문자열. enum 아님.
- `User.tier` — `"FREE"` | `"ADMIN"` 문자열. ADMIN 여부는 `"ADMIN".equals(user.getTier())`로 확인.

## 레포지토리 주의사항

- UPDATE 쿼리는 반드시 `@Modifying` + `@Query` 조합. `@Modifying` 없으면 실행 안 됨.
- JPA 없음 — 엔티티 간 연관 조회는 `flatMap` 체인으로 수동 처리.
- `BillingLogRepository.findDailyStatsByUserId`: `billing_logs`와 `api_keys`를 JOIN해서 특정 유저의 집계만 반환.

## DailyStat이 core에 있는 이유

`BillingLogRepository(@Query)`의 반환 타입. 레포지토리가 core에 있으므로 참조 타입도 core에 있어야 함. gateway로 이동하면 `core → gateway` 역의존 발생.
