package com.hypepia.apiverse.core.repository;

import com.hypepia.apiverse.core.dto.DailyStat;
import com.hypepia.apiverse.core.entity.BillingLog;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface BillingLogRepository extends ReactiveCrudRepository<BillingLog, Long> {
    Flux<BillingLog> findByApiKeyValue(String apiKeyValue);

    @Query("""
        SELECT
            TO_CHAR(request_time, 'MM/DD') AS date,
            COUNT(*) FILTER (WHERE response_status < 500) AS requests,
            COUNT(*) FILTER (WHERE response_status >= 500) AS errors
        FROM billing_logs
        WHERE request_time >= NOW() - INTERVAL '7 days'
        GROUP BY DATE(request_time), TO_CHAR(request_time, 'MM/DD')
        ORDER BY DATE(request_time)
        """)
    Flux<DailyStat> findDailyStats();
}
