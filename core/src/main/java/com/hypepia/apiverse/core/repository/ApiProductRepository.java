package com.hypepia.apiverse.core.repository;

import com.hypepia.apiverse.core.entity.ApiProduct;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ApiProductRepository extends ReactiveCrudRepository<ApiProduct, Long> {
    Flux<ApiProduct> findAllByIsActiveTrueOrderByIsPremiumAsc();
    Flux<ApiProduct> findAllByIsActiveFalseOrderByIdDesc();
    Flux<ApiProduct> findAllByOrderByIdDesc();
    Flux<ApiProduct> findAllByCreatedByOrderByIdDesc(Long createdBy);
    Mono<ApiProduct> findByCode(String code);
}
