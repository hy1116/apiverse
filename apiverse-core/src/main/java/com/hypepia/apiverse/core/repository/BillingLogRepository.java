package com.hypepia.apiverse.core.repository;

import com.hypepia.apiverse.core.entity.BillingLog;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface BillingLogRepository extends ReactiveCrudRepository<BillingLog, Long> {
    Flux<BillingLog> findByApiKeyValue(String apiKeyValue);
}
