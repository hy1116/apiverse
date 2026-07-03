package com.hypepia.apiverse.gateway.key;

import com.hypepia.apiverse.core.entity.ApiKey;
import com.hypepia.apiverse.core.repository.ApiKeyRepository;
import com.hypepia.apiverse.core.repository.ApiProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public Flux<Map<String, Object>> listKeys(@AuthenticationPrincipal Mono<Long> principal) {
        return principal
                .flatMapMany(userId -> apiKeyRepository.findByUserId(userId)
                        .filter(key -> Boolean.TRUE.equals(key.getIsActive()))
                        .flatMap(key -> apiProductRepository.findById(key.getApiProductId())
                                .map(product -> toKeyMap(key, product.getName()))
                                .defaultIfEmpty(toKeyMap(key, "Unknown"))
                        )
                );
    }

    @PostMapping
    public Mono<Map<String, Object>> issueKey(@RequestBody Map<String, Object> body,
                                              @AuthenticationPrincipal Mono<Long> principal) {
        Long apiProductId = Long.parseLong(String.valueOf(body.get("apiProductId")));
        return principal
                .flatMap(userId -> apiKeyRepository.findByUserIdAndApiProductId(userId, apiProductId)
                        .flatMap(existing -> {
                            if (Boolean.TRUE.equals(existing.getIsActive())) {
                                return Mono.error(
                                        new ResponseStatusException(HttpStatus.CONFLICT, "이미 발급된 키가 있습니다."));
                            }
                            // 폐기 후 재발급: usedQuota는 user+product 단위로 유지되어야 하므로
                            // 새 row를 만들지 않고 기존 row를 재활성화한다 (폐기→재발급으로 월 쿼터를 우회하는 것 방지)
                            existing.setApiKeyValue("apiverse_sandbox_" + UUID.randomUUID().toString().replace("-", ""));
                            existing.setIsActive(true);
                            return apiKeyRepository.save(existing);
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            String keyValue = "apiverse_sandbox_" + UUID.randomUUID().toString().replace("-", "");
                            ApiKey apiKey = ApiKey.builder()
                                    .userId(userId)
                                    .apiProductId(apiProductId)
                                    .apiKeyValue(keyValue)
                                    .build();
                            return apiKeyRepository.save(apiKey);
                        }))
                        .flatMap(saved -> apiProductRepository.findById(apiProductId)
                                .map(product -> toKeyMap(saved, product.getName()))
                                .defaultIfEmpty(toKeyMap(saved, "Unknown"))
                        )
                );
    }

    @DeleteMapping("/{id}")
    public Mono<Void> revokeKey(@PathVariable Long id, @AuthenticationPrincipal Mono<Long> principal) {
        return principal
                .flatMap(userId -> apiKeyRepository.findById(id)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                        .flatMap(key -> {
                            if (!userId.equals(key.getUserId())) {
                                return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
                            }
                            // 하드 삭제 대신 비활성화만 한다: usedQuota/monthlyQuota를 보존해야
                            // 재발급 시 쿼터가 리셋되는 것을 막을 수 있다
                            key.setIsActive(false);
                            return apiKeyRepository.save(key);
                        })
                        .then()
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
