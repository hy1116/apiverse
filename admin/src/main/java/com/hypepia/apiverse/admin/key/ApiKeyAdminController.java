package com.hypepia.apiverse.admin.key;

import com.hypepia.apiverse.core.entity.ApiKey;
import com.hypepia.apiverse.core.entity.ApiProduct;
import com.hypepia.apiverse.core.entity.User;
import com.hypepia.apiverse.core.repository.ApiKeyRepository;
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
@RequestMapping("/api/admin/keys")
@RequiredArgsConstructor
public class ApiKeyAdminController {

    private final ApiKeyRepository apiKeyRepository;
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
    public Flux<Map<String, Object>> list(@AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMapMany(uid -> apiKeyRepository.findAllByOrderByIdDesc())
                .flatMap(key -> Mono.zip(
                                apiProductRepository.findById(key.getApiProductId())
                                        .map(ApiProduct::getName)
                                        .defaultIfEmpty("Unknown"),
                                userRepository.findById(key.getUserId())
                                        .map(User::getEmail)
                                        .defaultIfEmpty("Unknown"))
                        .map(tuple -> toKeyMap(key, tuple.getT1(), tuple.getT2())));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> revoke(@PathVariable Long id,
                                             @AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMap(uid -> apiKeyRepository.findById(id))
                .flatMap(key -> apiKeyRepository.deleteById(id)
                        .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT).<Void>build()))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    private Map<String, Object> toKeyMap(ApiKey key, String productName, String userEmail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",             key.getId());
        m.put("userId",         key.getUserId());
        m.put("userEmail",      userEmail);
        m.put("apiKeyValue",    key.getApiKeyValue());
        m.put("apiProductId",   key.getApiProductId());
        m.put("apiProductName", productName);
        m.put("monthlyQuota",   key.getMonthlyQuota());
        m.put("usedQuota",      key.getUsedQuota());
        m.put("isActive",       key.getIsActive());
        return m;
    }
}
