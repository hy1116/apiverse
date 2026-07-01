package com.hypepia.apiverse.core.repository;

import com.hypepia.apiverse.core.projection.DailyStat;
import com.hypepia.apiverse.core.entity.BillingLog;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface BillingLogRepository extends ReactiveCrudRepository<BillingLog, Long> {
    Flux<BillingLog> findByApiKeyValue(String apiKeyValue);

    @Query("""
        SELECT
            TO_CHAR(bl.request_time, 'MM/DD') AS date,
            COUNT(*) FILTER (WHERE bl.response_status < 500) AS requests,
            COUNT(*) FILTER (WHERE bl.response_status >= 500) AS errors
        FROM billing_logs bl
        JOIN api_keys ak ON bl.api_key_value = ak.api_key_value
        WHERE bl.request_time >= NOW() - INTERVAL '7 days'
          AND ak.user_id = :userId
        GROUP BY DATE(bl.request_time), TO_CHAR(bl.request_time, 'MM/DD')
        ORDER BY DATE(bl.request_time)
        """)
    Flux<DailyStat> findDailyStatsByUserId(Long userId);
}
