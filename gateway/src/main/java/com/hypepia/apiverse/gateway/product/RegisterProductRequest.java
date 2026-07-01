package com.hypepia.apiverse.gateway.product;

public record RegisterProductRequest(
        String name,
        String description,
        String baseUrl,
        String category,
        Integer callsPerSec,
        Boolean isPremium,
        String specJson
) {}
