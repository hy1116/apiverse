package com.hypepia.apiverse.core.repository;

import com.hypepia.apiverse.core.entity.BlockedIp;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BlockedIpRepository extends ReactiveCrudRepository<BlockedIp, Long> {
    Flux<BlockedIp> findAllByOrderByIdDesc();
    Mono<BlockedIp> findByIpAddress(String ipAddress);
}
