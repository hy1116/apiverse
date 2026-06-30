package com.hypepia.apiverse.core.repository;

import com.hypepia.apiverse.core.entity.ApiProduct;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ApiProductRepository extends ReactiveCrudRepository<ApiProduct, Long> {
    Flux<ApiProduct> findByIsActiveTrue();
}
