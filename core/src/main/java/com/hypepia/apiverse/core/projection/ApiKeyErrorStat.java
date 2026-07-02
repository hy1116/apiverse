package com.hypepia.apiverse.core.projection;

public record ApiKeyErrorStat(
        String apiKeyValue,
        String userEmail,
        String productName,
        Long totalRequests,
        Long errorCount
) {}
