package com.hypepia.apiverse.admin.product;

public record UpdateProductRequest(
        String description,
        String baseUrl,
        String category,
        Integer callsPerSec,
        Boolean isPremium,
        String responseType,
        String specJson,
        String code,
        String upstreamApiKey,
        String upstreamKeyParam
) {}
