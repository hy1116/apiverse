package com.hypepia.apiverse.gateway.proxy;

import com.hypepia.apiverse.core.entity.ApiKey;
import com.hypepia.apiverse.core.entity.BillingLog;
import com.hypepia.apiverse.core.repository.ApiKeyRepository;
import com.hypepia.apiverse.core.repository.ApiProductRepository;
import com.hypepia.apiverse.core.repository.BillingLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiProductRepository apiProductRepository;
    private final BillingLogRepository billingLogRepository;
    private final RateLimiter rateLimiter;
    private final WebClient.Builder webClientBuilder;

    public Mono<ResponseEntity<String>> proxy(ServerWebExchange exchange, Long productId) {
        String apiKeyValue = exchange.getRequest().getHeaders().getFirst("X-API-KEY");

        if (apiKeyValue == null || apiKeyValue.isBlank()) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("X-API-KEY header is required"));
        }

        String clientIp = resolveClientIp(exchange);

        return apiKeyRepository.findByApiKeyValue(apiKeyValue)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key")))
                .flatMap(apiKey -> validateKey(apiKey, productId, clientIp))
                .flatMap(apiKey -> apiProductRepository.findById(productId)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")))
                        .flatMap(product -> rateLimiter.isAllowed(apiKeyValue, product.getCallsPerSec())
                                .flatMap(allowed -> {
                                    if (!allowed) {
                                        return Mono.just(
                                                ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit exceeded"));
                                    }
                                    return forward(exchange, product.getBaseUrl(), apiKey)
                                            .doOnNext(response -> {
                                                writeBillingLog(exchange, apiKeyValue, response);
                                                apiKeyRepository.incrementUsedQuota(apiKey.getId())
                                                        .doOnError(e -> log.error("Failed to increment used_quota for key {}: {}", apiKey.getId(), e.getMessage()))
                                                        .subscribe();
                                            });
                                })
                        )
                )
                .onErrorResume(ResponseStatusException.class, ex ->
                        Mono.just(ResponseEntity.status(ex.getStatusCode()).body(ex.getReason())));
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        return remote != null ? remote.getAddress().getHostAddress() : "unknown";
    }

    private Mono<ApiKey> validateKey(ApiKey apiKey, Long productId, String clientIp) {
        if (!Boolean.TRUE.equals(apiKey.getIsActive())) {
            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "API key is inactive"));
        }
        if (!apiKey.getApiProductId().equals(productId)) {
            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Key is not authorized for this product"));
        }
        String whiteListIp = apiKey.getWhiteListIp();
        if (whiteListIp != null && !whiteListIp.isBlank() && !whiteListIp.equals(clientIp)) {
            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "IP not whitelisted"));
        }
        int quota = apiKey.getMonthlyQuota() != null ? apiKey.getMonthlyQuota() : -1;
        int used  = apiKey.getUsedQuota()   != null ? apiKey.getUsedQuota()   : 0;
        if (quota != -1 && used >= quota) {
            return Mono.error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Monthly quota exceeded"));
        }
        return Mono.just(apiKey);
    }

    private Mono<ResponseEntity<String>> forward(ServerWebExchange exchange, String baseUrl, ApiKey apiKey) {
        ServerHttpRequest req = exchange.getRequest();
        String subPath   = req.getPath().value().replaceFirst("^/gateway/[^/]+", "");
        String rawQuery  = req.getURI().getRawQuery();
        String targetUri = baseUrl + subPath + (rawQuery != null ? "?" + rawQuery : "");

        HttpHeaders headers = new HttpHeaders();
        req.getHeaders().forEach((name, values) -> {
            if (!name.equalsIgnoreCase("Host") && !name.equalsIgnoreCase("X-API-KEY")) {
                headers.addAll(name, values);
            }
        });
        headers.set("X-API-KEY", apiKey.getApiKeyValue());

        return webClientBuilder.build()
                .method(req.getMethod())
                .uri(targetUri)
                .headers(h -> h.addAll(headers))
                .body(req.getBody(), DataBuffer.class)
                .exchangeToMono(res -> res.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> ResponseEntity.status(res.statusCode())
                                .contentType(res.headers().contentType().orElse(MediaType.APPLICATION_JSON))
                                .body(body)))
                .onErrorResume(WebClientRequestException.class, e -> {
                    log.error("Upstream request failed for {}: {}", targetUri, e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Upstream unavailable"));
                });
    }

    private void writeBillingLog(ServerWebExchange exchange, String apiKeyValue,
                                  ResponseEntity<String> response) {
        String clientIp = resolveClientIp(exchange);

        BillingLog entry = BillingLog.builder()
                .apiKeyValue(apiKeyValue)
                .requestPath(exchange.getRequest().getPath().value())
                .httpMethod(exchange.getRequest().getMethod().name())
                .responseStatus(response.getStatusCode().value())
                .clientIp(clientIp)
                .requestTime(LocalDateTime.now())
                .build();

        billingLogRepository.save(entry)
                .doOnError(e -> log.error("Failed to write billing log: {}", e.getMessage()))
                .subscribe();
    }
}
