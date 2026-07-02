package com.hypepia.apiverse.core.repository;

import com.hypepia.apiverse.core.projection.ApiKeyErrorStat;
import com.hypepia.apiverse.core.projection.DailyStat;
import com.hypepia.apiverse.core.projection.ProductErrorStat;
import com.hypepia.apiverse.core.projection.StatusCodeStat;
import com.hypepia.apiverse.core.entity.BillingLog;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BillingLogRepository extends ReactiveCrudRepository<BillingLog, Long> {

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

    @Query("""
        SELECT
            TO_CHAR(bl.request_time, 'MM/DD') AS date,
            COUNT(*) FILTER (WHERE bl.response_status < 500) AS requests,
            COUNT(*) FILTER (WHERE bl.response_status >= 500) AS errors
        FROM billing_logs bl
        WHERE bl.request_time >= NOW() - INTERVAL '7 days'
        GROUP BY DATE(bl.request_time), TO_CHAR(bl.request_time, 'MM/DD')
        ORDER BY DATE(bl.request_time)
        """)
    Flux<DailyStat> findDailyStatsGlobal();

    // onlyErrors=true면 5xx(response_status>=500) 건만, false면 전체 반환 (usage/daily의 errors 정의와 동일)
    // days로 조회 기간 상한을 둔다 — 무제한 스캔 방지 (컨트롤러에서 1~90으로 clamp)
    @Query("""
        SELECT * FROM billing_logs
        WHERE (:onlyErrors = FALSE OR response_status >= 500)
          AND request_time >= NOW() - (INTERVAL '1 day' * :days)
        ORDER BY request_time DESC
        LIMIT :size OFFSET :offset
        """)
    Flux<BillingLog> findLogsPage(boolean onlyErrors, int days, int size, long offset);

    @Query("""
        SELECT COUNT(*) FROM billing_logs
        WHERE (:onlyErrors = FALSE OR response_status >= 500)
          AND request_time >= NOW() - (INTERVAL '1 day' * :days)
        """)
    Mono<Long> countLogs(boolean onlyErrors, int days);

    // 상품별 에러율 랭킹 — 호출량 대비 5xx 비율이 가장 높은(가장 불안정한) 상품 순
    @Query("""
        SELECT p.name AS product_name, p.code AS product_code,
               COUNT(*) AS total_requests,
               COUNT(*) FILTER (WHERE bl.response_status >= 500) AS error_count
        FROM billing_logs bl
        JOIN api_keys ak ON bl.api_key_value = ak.api_key_value
        JOIN api_products p ON ak.api_product_id = p.id
        WHERE bl.request_time >= NOW() - (INTERVAL '1 day' * :days)
        GROUP BY p.id, p.name, p.code
        ORDER BY (COUNT(*) FILTER (WHERE bl.response_status >= 500)::float / COUNT(*)) DESC, error_count DESC
        """)
    Flux<ProductErrorStat> findProductErrorStats(int days);

    // 에러를 가장 많이 겪는 API 키 Top N — 어느 클라이언트(연동사)에서 문제가 나는지
    @Query("""
        SELECT bl.api_key_value AS api_key_value, u.email AS user_email, p.name AS product_name,
               COUNT(*) AS total_requests,
               COUNT(*) FILTER (WHERE bl.response_status >= 500) AS error_count
        FROM billing_logs bl
        JOIN api_keys ak ON bl.api_key_value = ak.api_key_value
        JOIN users u ON ak.user_id = u.id
        JOIN api_products p ON ak.api_product_id = p.id
        WHERE bl.request_time >= NOW() - (INTERVAL '1 day' * :days)
        GROUP BY bl.api_key_value, u.email, p.name
        HAVING COUNT(*) FILTER (WHERE bl.response_status >= 500) > 0
        ORDER BY error_count DESC
        LIMIT :limit
        """)
    Flux<ApiKeyErrorStat> findTopErrorApiKeys(int days, int limit);

    // 상태코드 분포 (4xx vs 5xx 등 구분에 사용)
    @Query("""
        SELECT response_status, COUNT(*) AS count
        FROM billing_logs
        WHERE request_time >= NOW() - (INTERVAL '1 day' * :days)
        GROUP BY response_status
        ORDER BY response_status
        """)
    Flux<StatusCodeStat> findStatusCodeStats(int days);

    // 상품별 사용량 랭킹 — findProductErrorStats와 동일 집계, 정렬만 호출량 기준
    @Query("""
        SELECT p.name AS product_name, p.code AS product_code,
               COUNT(*) AS total_requests,
               COUNT(*) FILTER (WHERE bl.response_status >= 500) AS error_count
        FROM billing_logs bl
        JOIN api_keys ak ON bl.api_key_value = ak.api_key_value
        JOIN api_products p ON ak.api_product_id = p.id
        WHERE bl.request_time >= NOW() - (INTERVAL '1 day' * :days)
        GROUP BY p.id, p.name, p.code
        ORDER BY total_requests DESC
        """)
    Flux<ProductErrorStat> findMostUsedProducts(int days);

    // 호출량이 가장 많은 API 키 Top N — findTopErrorApiKeys와 동일 집계, 에러 유무와 무관하게 전체 대상
    @Query("""
        SELECT bl.api_key_value AS api_key_value, u.email AS user_email, p.name AS product_name,
               COUNT(*) AS total_requests,
               COUNT(*) FILTER (WHERE bl.response_status >= 500) AS error_count
        FROM billing_logs bl
        JOIN api_keys ak ON bl.api_key_value = ak.api_key_value
        JOIN users u ON ak.user_id = u.id
        JOIN api_products p ON ak.api_product_id = p.id
        WHERE bl.request_time >= NOW() - (INTERVAL '1 day' * :days)
        GROUP BY bl.api_key_value, u.email, p.name
        ORDER BY total_requests DESC
        LIMIT :limit
        """)
    Flux<ApiKeyErrorStat> findMostActiveApiKeys(int days, int limit);

    // 본인 소유 API 키의 로그만 조회 (web의 /api/usage/logs) — apiProductId가 null이면 전체 상품 대상
    // days로 조회 기간 상한을 둔다 — 무제한 스캔 방지 (컨트롤러에서 1~90으로 clamp)
    @Query("""
        SELECT bl.* FROM billing_logs bl
        JOIN api_keys ak ON bl.api_key_value = ak.api_key_value
        WHERE ak.user_id = :userId
          AND ak.api_product_id = COALESCE(:apiProductId, ak.api_product_id)
          AND (:onlyErrors = FALSE OR bl.response_status >= 500)
          AND bl.request_time >= NOW() - (INTERVAL '1 day' * :days)
        ORDER BY bl.request_time DESC
        LIMIT :size OFFSET :offset
        """)
    Flux<BillingLog> findLogsPageByUserId(Long userId, Long apiProductId, boolean onlyErrors, int days, int size, long offset);

    @Query("""
        SELECT COUNT(*) FROM billing_logs bl
        JOIN api_keys ak ON bl.api_key_value = ak.api_key_value
        WHERE ak.user_id = :userId
          AND ak.api_product_id = COALESCE(:apiProductId, ak.api_product_id)
          AND (:onlyErrors = FALSE OR bl.response_status >= 500)
          AND bl.request_time >= NOW() - (INTERVAL '1 day' * :days)
        """)
    Mono<Long> countLogsByUserId(Long userId, Long apiProductId, boolean onlyErrors, int days);
}
