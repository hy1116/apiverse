package com.hypepia.apiverse.gateway.proxy;

import com.hypepia.apiverse.core.entity.ApiKey;
import com.hypepia.apiverse.core.entity.ApiProduct;
import com.hypepia.apiverse.core.entity.BillingLog;
import com.hypepia.apiverse.core.repository.ApiKeyRepository;
import com.hypepia.apiverse.core.repository.ApiProductRepository;
import com.hypepia.apiverse.core.repository.BillingLogRepository;
import com.hypepia.apiverse.core.repository.BlockedIpRepository;
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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiProductRepository apiProductRepository;
    private final BillingLogRepository billingLogRepository;
    private final BlockedIpRepository blockedIpRepository;
    private final RateLimiter rateLimiter;
    private final WebClient.Builder webClientBuilder;

    public Mono<ResponseEntity<String>> proxy(ServerWebExchange exchange, String code) {
        String clientIp = resolveClientIp(exchange);

        return blockedIpRepository.findByIpAddress(clientIp)
                .<ResponseEntity<String>>flatMap(blocked -> Mono.just(
                        ResponseEntity.status(HttpStatus.FORBIDDEN).body("IP blocked")))
                .switchIfEmpty(Mono.defer(() -> doProxy(exchange, code, clientIp)));
    }

    private Mono<ResponseEntity<String>> doProxy(ServerWebExchange exchange, String code, String clientIp) {
        String apiKeyValue = exchange.getRequest().getHeaders().getFirst("X-API-KEY");

        if (apiKeyValue == null || apiKeyValue.isBlank()) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("X-API-KEY header is required"));
        }

        return apiProductRepository.findByCode(code)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")))
                .flatMap(product -> apiKeyRepository.findByApiKeyValue(apiKeyValue)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API key")))
                        .flatMap(apiKey -> validateKey(apiKey, product.getId(), clientIp))
                        .flatMap(apiKey -> rateLimiter.isAllowed(apiKeyValue, product.getCallsPerSec())
                                .flatMap(allowed -> {
                                    if (!allowed) {
                                        return Mono.just(
                                                ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit exceeded"));
                                    }
                                    return forward(exchange, product, apiKey)
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
        return remote != null ? toIPv4IfPossible(remote.getAddress()) : "unknown";
    }

    // 로컬 개발 환경 등에서 소켓 주소가 IPv6(::1, ::ffff:a.b.c.d)로 잡히는 경우를 IPv4 표기로 정규화
    // — 화이트리스트/차단 목록에 등록하는 IP는 보통 IPv4라서 표기가 다르면 매칭이 실패한다.
    static String toIPv4IfPossible(InetAddress address) {
        if (!(address instanceof Inet6Address v6)) {
            return address.getHostAddress();
        }
        if (v6.isLoopbackAddress()) {
            return "127.0.0.1";
        }
        byte[] bytes = v6.getAddress();
        if (bytes.length == 16 && bytes[10] == (byte) 0xff && bytes[11] == (byte) 0xff) {
            try {
                return InetAddress.getByAddress(new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]}).getHostAddress();
            } catch (UnknownHostException e) {
                return address.getHostAddress();
            }
        }
        return address.getHostAddress();
    }

    private Mono<ApiKey> validateKey(ApiKey apiKey, Long productId, String clientIp) {
        if (!Boolean.TRUE.equals(apiKey.getIsActive())) {
            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "API key is inactive"));
        }
        if (!apiKey.getApiProductId().equals(productId)) {
            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Key is not authorized for this product"));
        }
        if (!isIpAllowed(apiKey.getWhiteListIp(), clientIp)) {
            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "IP not whitelisted"));
        }
        int quota = apiKey.getMonthlyQuota() != null ? apiKey.getMonthlyQuota() : -1;
        int used  = apiKey.getUsedQuota()   != null ? apiKey.getUsedQuota()   : 0;
        if (quota != -1 && used >= quota) {
            return Mono.error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Monthly quota exceeded"));
        }
        return Mono.just(apiKey);
    }

    // whiteListIp가 비어있으면 전체 허용. 콤마로 여러 IP를 등록할 수 있음 (예: "1.2.3.4, 5.6.7.8")
    static boolean isIpAllowed(String whiteListIp, String clientIp) {
        if (whiteListIp == null || whiteListIp.isBlank()) {
            return true;
        }
        for (String allowed : whiteListIp.split(",")) {
            if (allowed.trim().equals(clientIp)) {
                return true;
            }
        }
        return false;
    }

    private Mono<ResponseEntity<String>> forward(ServerWebExchange exchange, ApiProduct product, ApiKey apiKey) {
        ServerHttpRequest req = exchange.getRequest();
        String subPath   = req.getPath().value().replaceFirst("^/gateway/[^/]+", "");
        String rawQuery  = req.getURI().getRawQuery();
        String baseTargetUri = product.getBaseUrl() + subPath + (rawQuery != null ? "?" + rawQuery : "");

        HttpHeaders headers = new HttpHeaders();
        req.getHeaders().forEach((name, values) -> {
            if (!name.equalsIgnoreCase("Host") && !name.equalsIgnoreCase("X-API-KEY")) {
                headers.addAll(name, values);
            }
        });
        headers.set("X-API-KEY", apiKey.getApiKeyValue());

        String targetUri = applyUpstreamKey(baseTargetUri, headers, product);

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
                .timeout(Duration.ofSeconds(15))
                .onErrorResume(WebClientRequestException.class, e -> {
                    log.error("Upstream request failed for {}: {}", targetUri, e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Upstream unavailable"));
                })
                .onErrorResume(TimeoutException.class, e -> {
                    log.error("Upstream request timed out for {}", targetUri);
                    return Mono.just(ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("Upstream timeout"));
                });
    }

    // upstreamKeyParam 형식: "header:{헤더명}" 또는 "query:{쿼리파라미터명}" — 형식이 이 두 가지가 아니거나
    // upstreamApiKey/upstreamKeyParam이 비어있으면 아무것도 주입하지 않음 (공개 API 등 키가 필요 없는 상품)
    private String applyUpstreamKey(String targetUri, HttpHeaders headers, ApiProduct product) {
        String upstreamKey = product.getUpstreamApiKey();
        String param = product.getUpstreamKeyParam();
        if (upstreamKey == null || upstreamKey.isBlank() || param == null || param.isBlank()) {
            return targetUri;
        }
        String[] parts = param.split(":", 2);
        if (parts.length != 2) {
            return targetUri;
        }
        String type = parts[0].trim();
        String name = parts[1].trim();
        if ("header".equalsIgnoreCase(type)) {
            headers.set(name, upstreamKey);
            return targetUri;
        }
        if ("query".equalsIgnoreCase(type)) {
            String separator = targetUri.contains("?") ? "&" : "?";
            return targetUri + separator + name + "=" + URLEncoder.encode(upstreamKey, StandardCharsets.UTF_8);
        }
        return targetUri;
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
