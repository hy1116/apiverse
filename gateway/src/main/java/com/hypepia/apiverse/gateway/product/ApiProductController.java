package com.hypepia.apiverse.gateway.product;

import com.hypepia.apiverse.core.entity.ApiProduct;
import com.hypepia.apiverse.core.repository.ApiKeyRepository;
import com.hypepia.apiverse.core.repository.ApiProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ApiProductController {

    private final ApiProductRepository apiProductRepository;
    private final ApiKeyRepository apiKeyRepository;

    @GetMapping
    public Flux<ApiProduct> listProducts() {
        return apiProductRepository.findAllByIsActiveTrueOrderByIsPremiumAsc();
    }

    @GetMapping("/{id}")
    public Mono<ApiProduct> getProduct(@PathVariable Long id) {
        return apiProductRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    // 로그인 상태이면 해당 유저의 키를 반환, 비로그인이면 {}
    @GetMapping("/{id}/my-key")
    public Mono<Map<String, String>> getMyKey(@PathVariable Long id) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (Long) ctx.getAuthentication().getPrincipal())
                .flatMap(userId -> apiKeyRepository.findByUserIdAndApiProductId(userId, id))
                .<Map<String, String>>map(key -> Map.of("apiKeyValue", key.getApiKeyValue()))
                .defaultIfEmpty(Map.of());
    }
}
