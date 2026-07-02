package com.hypepia.apiverse.core.repository;

import com.hypepia.apiverse.core.entity.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByEmail(String email);
    Flux<User> findAllByOrderByIdDesc();
}
