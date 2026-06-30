package com.hypepia.apiverse.core.repository;

import com.hypepia.apiverse.core.entity.ApiKey;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ApiKeyRepository extends ReactiveCrudRepository<ApiKey, Long> {
    Mono<ApiKey> findByApiKeyValue(String apiKeyValue);
    Flux<ApiKey> findByUserId(Long userId);
    Mono<ApiKey> findByUserIdAndApiProductId(Long userId, Long apiProductId);
}
