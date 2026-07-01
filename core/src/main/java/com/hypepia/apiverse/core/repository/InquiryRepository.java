package com.hypepia.apiverse.core.repository;

import com.hypepia.apiverse.core.entity.Inquiry;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface InquiryRepository extends ReactiveCrudRepository<Inquiry, Long> {
    Flux<Inquiry> findByUserIdOrderByCreatedAtDesc(Long userId);
}
