package com.hypepia.apiverse.core.repository;

import com.hypepia.apiverse.core.entity.ApiKey;
import com.hypepia.apiverse.core.projection.QuotaUsageStat;
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

    // 월 쿼터 사용률 Top N — monthly_quota=-1(무제한) 제외, used/monthly 비율 내림차순
    @Query("""
        SELECT ak.api_key_value AS api_key_value, u.email AS user_email, p.name AS product_name,
               ak.monthly_quota AS monthly_quota, ak.used_quota AS used_quota
        FROM api_keys ak
        JOIN users u ON ak.user_id = u.id
        JOIN api_products p ON ak.api_product_id = p.id
        WHERE ak.monthly_quota <> -1 AND ak.monthly_quota > 0
        ORDER BY (ak.used_quota::float / ak.monthly_quota) DESC
        LIMIT :limit
        """)
    Flux<QuotaUsageStat> findQuotaUsageStats(int limit);
}
