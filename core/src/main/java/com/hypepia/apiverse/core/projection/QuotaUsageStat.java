package com.hypepia.apiverse.core.projection;

public record QuotaUsageStat(
        String apiKeyValue,
        String userEmail,
        String productName,
        Integer monthlyQuota,
        Integer usedQuota
) {}
