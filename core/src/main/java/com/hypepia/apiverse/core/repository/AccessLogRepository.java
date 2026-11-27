package com.hypepia.apiverse.core.repository;

import com.hypepia.apiverse.core.entity.AccessLog;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface AccessLogRepository extends ReactiveCrudRepository<AccessLog, Long> {
}
