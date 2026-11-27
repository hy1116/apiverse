package com.hypepia.apiverse.batch.settlement;

import java.time.LocalDate;

// api_key_monthly_usage 한 행 — 키별 월 사용량 스냅샷
public record MonthlyUsage(
        LocalDate usageMonth,   // 해당 월 1일
        long apiKeyId,
        Long userId,
        Long apiProductId,
        int monthlyQuota,       // -1 = 무제한
        int usedQuota,          // 게이트웨이 카운터(api_keys.used_quota) 값
        int overQuota,          // 쿼터 초과분 — 추후 초과 과금 근거
        long requestCount,      // billing_logs 기준 전체 호출 수 (401/429 등 포함)
        long errorCount         // billing_logs 기준 5xx 수
) {

    // reader가 읽은 원본 행 (월/초과분 계산 전)
    public record Row(long apiKeyId, Long userId, Long apiProductId,
                      int monthlyQuota, int usedQuota, long requestCount, long errorCount) {
    }

    public static MonthlyUsage of(LocalDate usageMonth, Row r) {
        int over = r.monthlyQuota() < 0 ? 0 : Math.max(0, r.usedQuota() - r.monthlyQuota());
        return new MonthlyUsage(usageMonth, r.apiKeyId(), r.userId(), r.apiProductId(),
                r.monthlyQuota(), r.usedQuota(), over, r.requestCount(), r.errorCount());
    }
}
