package com.hypepia.apiverse.admin.product;

public record UpdateProductRequest(
        String description,
        String baseUrl,
        String category,
        Integer callsPerSec,
        Boolean isPremium,
        String specJson
) {}
