package com.hypepia.apiverse.core.projection;

public record ProductErrorStat(
        String productName,
        String productCode,
        Long totalRequests,
        Long errorCount
) {}
