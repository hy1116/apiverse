# core 모듈

Spring Boot 플러그인 없는 순수 Java 라이브러리. gateway가 의존하는 공유 인프라.

## 패키지 구조

```
core/
├── config/      R2dbcRepositoryConfig (@EnableR2dbcRepositories)
├── entity/      User, ApiProduct, ApiKey, BillingLog, Inquiry, BlockedIp
├── projection/  DailyStat, ProductErrorStat, ApiKeyErrorStat, StatusCodeStat, QuotaUsageStat (SQL 쿼리 결과 매핑)
└── repository/  UserRepository, ApiProductRepository, ApiKeyRepository,
                 BillingLogRepository, InquiryRepository, BlockedIpRepository
```

## 엔티티 주의사항

- `ApiProduct.specJson` — `@JsonRawValue`: JSON 문자열을 이스케이프 없이 응답에 삽입. SwaggerUI 파싱에 필수.
- `ApiProduct`, `Inquiry` — `@Builder(toBuilder = true)`: 부분 업데이트(승인, 답변 저장)에 사용.
- `ApiKey.monthlyQuota` — `-1`은 무제한.
- `Inquiry.status` — `"PENDING"` | `"ANSWERED"` 문자열. enum 아님.
- `User.role` — `"USER"` | `"ADMIN"` 문자열. ADMIN 여부는 `"ADMIN".equals(user.getRole())`로 확인 (`tier`는 과금 등급 전용이라 권한 판별에 쓰지 않음 — `admin.md`의 role vs tier 참고).
- `User.updatedAt` — DB 기본값 `CURRENT_TIMESTAMP`(가입 시점)이며, 이후 `gateway.profile.ProfileController.update()`가 companyName/phone 수정 시마다 명시적으로 갱신한다 (JPA Auditing 없이 수동 관리).
- `BlockedIp` — `/gateway/**` 전역 차단 IP 목록. `ApiKey.whiteListIp`(회원별 허용 목록)와 반대 개념이며 서로 독립적.

## 레포지토리 주의사항

- UPDATE 쿼리는 반드시 `@Modifying` + `@Query` 조합. `@Modifying` 없으면 실행 안 됨.
- JPA 없음 — 엔티티 간 연관 조회는 `flatMap` 체인으로 수동 처리.
- `BillingLogRepository.findDailyStatsByUserId`: `billing_logs`와 `api_keys`를 JOIN해서 특정 유저의 집계만 반환.
- `BillingLogRepository.findLogsPage`/`countLogs`: LIMIT/OFFSET 기반 원본 로그 페이지네이션. `onlyErrors` boolean 파라미터로 `response_status >= 500` 필터링.
- `BillingLogRepository.findLogsPageByUserId`/`countLogsByUserId`(web의 `/api/usage/logs`용): 같은 named parameter(`:apiProductId`)를 쿼리 안에서 두 번 참조(`AND (:apiProductId IS NULL OR ak.api_product_id = :apiProductId)`)하면 Spring Data R2DBC가 `LIMIT :size OFFSET :offset`의 `:offset` 바인딩을 못 찾는 오류(`No parameter specified for [offset]`)를 내는 문제가 있었다 — 파라미터 개수가 5개로 이 리포지토리에서 가장 많고 그중 하나가 중복 참조된 조합에서만 재현됨. `ak.api_product_id = COALESCE(:apiProductId, ak.api_product_id)`로 바꿔 같은 named parameter를 한 번만 참조하도록 고쳐서 해결했다. 같은 파라미터를 쿼리에서 두 번 이상 쓰는 `@Query`를 추가할 때는 주의.

## DailyStat이 core에 있는 이유

`BillingLogRepository(@Query)`의 반환 타입. 레포지토리가 core에 있으므로 참조 타입도 core에 있어야 함. gateway로 이동하면 `core → gateway` 역의존 발생. `ProductErrorStat`/`ApiKeyErrorStat`/`StatusCodeStat`도 같은 이유로 core에 위치 (admin 전용 통계지만 레포지토리와 같은 모듈에 있어야 함).

## @Query에서 record 필드 매핑 규칙

네이티브 SQL 결과를 record projection에 매핑할 때 컬럼명은 record 필드명의 snake_case와 일치해야 한다 (예: `productName` 필드 ↔ SQL의 `product_name` 컬럼 별칭). `DailyStat`처럼 필드명이 단어 하나뿐이면 대소문자만 다르므로 눈에 안 띄지만, 여러 단어로 된 필드(`ProductErrorStat.productName` 등)는 SQL에 반드시 `AS product_name`처럼 snake_case로 별칭을 줘야 매핑된다.
