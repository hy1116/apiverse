package com.hypepia.apiverse.admin.product;

import com.hypepia.apiverse.core.entity.ApiProduct;
import com.hypepia.apiverse.core.repository.ApiProductRepository;
import com.hypepia.apiverse.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class ProductAdminController {

    private final ApiProductRepository apiProductRepository;
    private final UserRepository userRepository;

    private Mono<Void> requireAdmin(Long uid) {
        return userRepository.findById(uid)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED)))
                .flatMap(user -> "ADMIN".equals(user.getRole())
                        ? Mono.empty()
                        : Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN)));
    }

    @GetMapping
    public Flux<ApiProduct> listAll(@AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMapMany(uid -> apiProductRepository.findAllByOrderByIdDesc());
    }

    @GetMapping("/pending")
    public Flux<ApiProduct> listPending(@AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMapMany(uid -> apiProductRepository.findAllByIsActiveFalseOrderByIdDesc());
    }

    @PatchMapping("/{id}/approve")
    public Mono<ResponseEntity<ApiProduct>> approve(@PathVariable Long id,
                                                    @AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMap(uid -> apiProductRepository.findById(id)
                        .flatMap(p -> apiProductRepository.save(p.toBuilder().isActive(true).build()))
                        .map(ResponseEntity::ok)
                        .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
    }

    // 승인 대기 중인 상품 반려 — 아직 활성화된 적 없는 상품이므로 삭제로 처리
    @DeleteMapping("/{id}/reject")
    public Mono<ResponseEntity<Void>> reject(@PathVariable Long id,
                                             @AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMap(uid -> apiProductRepository.findById(id))
                .flatMap(p -> {
                    if (Boolean.TRUE.equals(p.getIsActive())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "이미 승인된 상품은 반려할 수 없습니다."));
                    }
                    return apiProductRepository.deleteById(id)
                            .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT).<Void>build());
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PatchMapping("/{id}")
    public Mono<ResponseEntity<ApiProduct>> update(@PathVariable Long id,
                                                   @RequestBody UpdateProductRequest req,
                                                   @AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMap(uid -> apiProductRepository.findById(id))
                .flatMap(p -> {
                    ApiProduct.ApiProductBuilder builder = p.toBuilder();
                    if (req.description() != null) builder.description(req.description());
                    if (req.baseUrl() != null) builder.baseUrl(req.baseUrl());
                    if (req.category() != null) builder.category(req.category());
                    if (req.callsPerSec() != null) builder.callsPerSec(req.callsPerSec());
                    if (req.isPremium() != null) builder.isPremium(req.isPremium());
                    if (req.specJson() != null) builder.specJson(req.specJson());
                    return apiProductRepository.save(builder.build());
                })
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
