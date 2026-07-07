package com.hypepia.apiverse.gateway.logging;

import com.hypepia.apiverse.core.entity.AccessLog;
import com.hypepia.apiverse.core.repository.AccessLogRepository;
import com.hypepia.apiverse.gateway.config.SecurityUtils;
import com.hypepia.apiverse.gateway.proxy.ProxyService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;

// gateway 앱 REST API(auth/product/key/inquiry 등) 전체에 대한 접근 로그.
// /gateway/** 프록시 호출은 ProxyService가 api_key_value 기준으로 billing_logs에 이미 기록하므로 여기서는 제외한다.
// JwtWebFilter와 마찬가지로 @Component로 두지 않고 AccessLoggingConfig에서 @Bean으로 등록한다 —
// @Component로 두면 @WebFluxTest 슬라이스가 WebFilter 빈을 자동으로 끌어와 AccessLogRepository 의존성을
// 찾지 못해 컨트롤러 단위 테스트가 깨진다.
@Slf4j
public class AccessLogWebFilter implements WebFilter {

    private final AccessLogRepository accessLogRepository;
    private final boolean trustForwardedHeaders;

    public AccessLogWebFilter(AccessLogRepository accessLogRepository, boolean trustForwardedHeaders) {
        this.accessLogRepository = accessLogRepository;
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (exchange.getRequest().getPath().value().startsWith("/gateway/")) {
            return chain.filter(exchange);
        }

        // 응답 커밋 직전에 걸어야 예외 핸들러가 매핑한 최종 상태 코드까지 정확히 기록된다
        // (핸들러 완료 시점에 걸면 에러 응답의 상태 코드가 아직 확정되지 않은 경우가 있다).
        exchange.getResponse().beforeCommit(() -> writeAccessLog(exchange));
        return chain.filter(exchange);
    }

    private Mono<Void> writeAccessLog(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        HttpStatusCode status = exchange.getResponse().getStatusCode();
        String clientIp = ProxyService.resolveClientIp(request, trustForwardedHeaders);

        return SecurityUtils.currentUserIdOrEmpty()
                .map(Optional::ofNullable)
                .defaultIfEmpty(Optional.empty())
                .flatMap(userId -> {
                    AccessLog entry = AccessLog.builder()
                            .source("GATEWAY")
                            .userId(userId.orElse(null))
                            .requestPath(request.getPath().value())
                            .httpMethod(request.getMethod().name())
                            .responseStatus(status != null ? status.value() : 0)
                            .clientIp(clientIp)
                            .requestTime(LocalDateTime.now())
                            .build();
                    return accessLogRepository.save(entry);
                })
                .doOnError(e -> log.error("Failed to write access log: {}", e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .then();
    }
}
