package com.hypepia.apiverse.gateway.product;

import com.hypepia.apiverse.core.entity.ApiProduct;
import com.hypepia.apiverse.core.repository.ApiKeyRepository;
import com.hypepia.apiverse.core.repository.ApiProductRepository;
import com.hypepia.apiverse.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ApiProductController {

    private final ApiProductRepository apiProductRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;

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
                .map(ctx -> (Long) Objects.requireNonNull(Objects.requireNonNull(ctx.getAuthentication()).getPrincipal()))
                .flatMap(userId -> apiKeyRepository.findByUserIdAndApiProductId(userId, id))
                .map(key -> Map.of("apiKeyValue", key.getApiKeyValue()))
                .defaultIfEmpty(Map.of());
    }

    // 인증된 사용자라면 누구나 등록 가능 — 승인 전까지 is_active = false
    @PostMapping
    public Mono<ResponseEntity<ApiProduct>> register(@RequestBody RegisterProductRequest req,
                                                     @AuthenticationPrincipal Mono<Long> principal) {
        if (req.name() == null || req.name().isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        if (req.baseUrl() == null || req.baseUrl().isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return principal.flatMap(uid -> {
            ApiProduct product = ApiProduct.builder()
                    .name(req.name().trim())
                    .description(req.description())
                    .baseUrl(req.baseUrl().trim())
                    .category(req.category())
                    .callsPerSec(req.callsPerSec() != null ? req.callsPerSec() : 5)
                    .isPremium(Boolean.TRUE.equals(req.isPremium()))
                    .isActive(false)
                    .specJson(req.specJson())
                    .build();
            return apiProductRepository.save(product)
                    .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved));
        });
    }

    // ADMIN 전용 — 승인 대기 중인 상품 목록
    @GetMapping("/pending")
    public Mono<ResponseEntity<Flux<ApiProduct>>> listPending(@AuthenticationPrincipal Mono<Long> principal) {
        return principal
                .flatMap(uid -> userRepository.findById(uid)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED))))
                .map(user -> {
                    if (!"ADMIN".equals(user.getRole())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                    }
                    return ResponseEntity.ok(apiProductRepository.findAllByIsActiveFalseOrderByIdDesc());
                });
    }

    // ADMIN 전용 — 상품 승인
    @PatchMapping("/{id}/approve")
    public Mono<ResponseEntity<ApiProduct>> approve(@PathVariable Long id,
                                                    @AuthenticationPrincipal Mono<Long> principal) {
        return principal
                .flatMap(uid -> userRepository.findById(uid)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED))))
                .flatMap(user -> {
                    if (!"ADMIN".equals(user.getRole())) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                    }
                    return apiProductRepository.findById(id)
                            .flatMap(p -> apiProductRepository.save(p.toBuilder().isActive(true).build()))
                            .map(ResponseEntity::ok)
                            .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
                });
    }
}
