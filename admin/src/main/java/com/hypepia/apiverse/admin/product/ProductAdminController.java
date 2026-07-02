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

import java.util.LinkedHashMap;
import java.util.Map;

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
    public Flux<Map<String, Object>> listAll(@AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMapMany(uid -> apiProductRepository.findAllByOrderByIdDesc())
                .map(ProductAdminController::toProductMap);
    }

    @GetMapping("/pending")
    public Flux<Map<String, Object>> listPending(@AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMapMany(uid -> apiProductRepository.findAllByIsActiveFalseOrderByIdDesc())
                .map(ProductAdminController::toProductMap);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> detail(@PathVariable Long id,
                                                            @AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMap(uid -> apiProductRepository.findById(id))
                .map(p -> ResponseEntity.ok(toProductMap(p)))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PatchMapping("/{id}/approve")
    public Mono<ResponseEntity<Map<String, Object>>> approve(@PathVariable Long id,
                                                             @AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMap(uid -> apiProductRepository.findById(id)
                        .flatMap(p -> apiProductRepository.save(p.toBuilder().isActive(true).build()))
                        .map(p -> ResponseEntity.ok(toProductMap(p)))
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
    public Mono<ResponseEntity<Map<String, Object>>> update(@PathVariable Long id,
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
                    if (req.responseType() != null) builder.responseType(req.responseType());
                    if (req.isPremium() != null) builder.isPremium(req.isPremium());
                    if (req.specJson() != null) builder.specJson(req.specJson());
                    if (req.code() != null) builder.code(req.code());
                    if (req.upstreamApiKey() != null) builder.upstreamApiKey(req.upstreamApiKey());
                    if (req.upstreamKeyParam() != null) builder.upstreamKeyParam(req.upstreamKeyParam());
                    return apiProductRepository.save(builder.build());
                })
                .map(p -> ResponseEntity.ok(toProductMap(p)))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // upstreamApiKey/upstreamKeyParam은 core.ApiProduct에서 @JsonIgnore 처리되어 있어
    // (일반 유저용 /api/products 응답에 절대 노출되지 않도록) 명시적으로 여기서만 담아 반환한다.
    private static Map<String, Object> toProductMap(ApiProduct p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",               p.getId());
        m.put("name",             p.getName());
        m.put("code",             p.getCode());
        m.put("description",      p.getDescription());
        m.put("baseUrl",          p.getBaseUrl());
        m.put("isPremium",        p.getIsPremium());
        m.put("isActive",         p.getIsActive());
        m.put("category",         p.getCategory());
        m.put("callsPerSec",      p.getCallsPerSec());
        m.put("responseType",     p.getResponseType());
        m.put("upstreamApiKey",   p.getUpstreamApiKey());
        m.put("upstreamKeyParam", p.getUpstreamKeyParam());
        return m;
    }
}
