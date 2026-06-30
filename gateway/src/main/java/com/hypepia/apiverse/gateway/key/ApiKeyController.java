package com.hypepia.apiverse.gateway.key;

import com.hypepia.apiverse.core.entity.ApiKey;
import com.hypepia.apiverse.core.repository.ApiKeyRepository;
import com.hypepia.apiverse.core.repository.ApiProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiProductRepository apiProductRepository;

    @GetMapping
    public Flux<Map<String, Object>> listKeys() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (Long) ctx.getAuthentication().getPrincipal())
                .flatMapMany(userId -> apiKeyRepository.findByUserId(userId)
                        .filter(key -> Boolean.TRUE.equals(key.getIsActive()))
                        .flatMap(key -> apiProductRepository.findById(key.getApiProductId())
                                .map(product -> toKeyMap(key, product.getName()))
                                .defaultIfEmpty(toKeyMap(key, "Unknown"))
                        )
                );
    }

    @PostMapping
    public Mono<Map<String, Object>> issueKey(@RequestBody Map<String, Object> body) {
        Long apiProductId = Long.parseLong(String.valueOf(body.get("apiProductId")));
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (Long) ctx.getAuthentication().getPrincipal())
                .flatMap(userId -> apiKeyRepository.findByUserIdAndApiProductId(userId, apiProductId)
                        .flatMap(existing -> Mono.<Map<String, Object>>error(
                                new ResponseStatusException(HttpStatus.CONFLICT, "이미 발급된 키가 있습니다.")))
                        .switchIfEmpty(Mono.defer(() -> {
                            String keyValue = "apiverse_sandbox_" + UUID.randomUUID().toString().replace("-", "");
                            ApiKey apiKey = ApiKey.builder()
                                    .userId(userId)
                                    .apiProductId(apiProductId)
                                    .apiKeyValue(keyValue)
                                    .build();
                            return apiKeyRepository.save(apiKey)
                                    .flatMap(saved -> apiProductRepository.findById(apiProductId)
                                            .map(product -> toKeyMap(saved, product.getName()))
                                            .defaultIfEmpty(toKeyMap(saved, "Unknown"))
                                    );
                        }))
                );
    }

    @DeleteMapping("/{id}")
    public Mono<Void> revokeKey(@PathVariable Long id) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (Long) ctx.getAuthentication().getPrincipal())
                .flatMap(userId -> apiKeyRepository.findById(id)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                        .flatMap(key -> {
                            if (!userId.equals(key.getUserId())) {
                                return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
                            }
                            return apiKeyRepository.deleteById(id);
                        })
                );
    }

    private Map<String, Object> toKeyMap(ApiKey key, String productName) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",             key.getId());
        m.put("apiKeyValue",    key.getApiKeyValue());
        m.put("apiProductId",   key.getApiProductId());
        m.put("apiProductName", productName);
        m.put("monthlyQuota",   key.getMonthlyQuota());
        m.put("usedQuota",      key.getUsedQuota());
        m.put("isActive",       key.getIsActive());
        return m;
    }
}
