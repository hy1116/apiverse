package com.hypepia.apiverse.core.repository;

import com.hypepia.apiverse.core.entity.ApiKey;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ApiKeyRepository extends ReactiveCrudRepository<ApiKey, Long> {
    Mono<ApiKey> findByApiKeyValue(String apiKeyValue);
    Flux<ApiKey> findByUserId(Long userId);
    Mono<ApiKey> findByUserIdAndApiProductId(Long userId, Long apiProductId);
    Flux<ApiKey> findAllByOrderByIdDesc();

    @Modifying
    @Query("UPDATE api_keys SET used_quota = used_quota + 1 WHERE id = :id")
    Mono<Void> incrementUsedQuota(Long id);

    @Modifying
    @Query("UPDATE api_keys SET used_quota = 0")
    Mono<Void> resetAllUsedQuota();
}
