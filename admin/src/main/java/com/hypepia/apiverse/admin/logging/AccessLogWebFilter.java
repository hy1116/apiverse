package com.hypepia.apiverse.admin.logging;

import com.hypepia.apiverse.core.entity.AccessLog;
import com.hypepia.apiverse.core.repository.AccessLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Optional;

// admin 앱(로그인 제외 전체가 ADMIN 인증 필요) REST API 전체에 대한 접근 로그.
// gateway의 AccessLogWebFilter와 동일 패턴 — 이 프로젝트는 JwtWebFilter/JwtUtils처럼 웹 계층 플러밍을
// core로 공유하지 않고 모듈별로 두므로(core는 R2DBC 엔티티/레포지토리 전용), IP 파싱 로직도 여기 복제한다.
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
        // 응답 커밋 직전에 걸어야 예외 핸들러가 매핑한 최종 상태 코드까지 정확히 기록된다.
        exchange.getResponse().beforeCommit(() -> writeAccessLog(exchange));
        return chain.filter(exchange);
    }

    private Mono<Void> writeAccessLog(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        HttpStatusCode status = exchange.getResponse().getStatusCode();
        String clientIp = resolveClientIp(request, trustForwardedHeaders);

        return currentUserIdOrEmpty()
                .map(Optional::ofNullable)
                .defaultIfEmpty(Optional.empty())
                .flatMap(userId -> {
                    AccessLog entry = AccessLog.builder()
                            .source("ADMIN")
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

    private static Mono<Long> currentUserIdOrEmpty() {
        return ReactiveSecurityContextHolder.getContext()
                .mapNotNull(SecurityContext::getAuthentication)
                .mapNotNull(auth -> (Long) auth.getPrincipal());
    }

    static String resolveClientIp(ServerHttpRequest req, boolean trustForwardedHeaders) {
        if (trustForwardedHeaders) {
            String forwarded = req.getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] parts = forwarded.split(",");
                return parts[parts.length - 1].trim();
            }
        }
        InetSocketAddress remote = req.getRemoteAddress();
        return remote != null ? toIPv4IfPossible(remote.getAddress()) : "unknown";
    }

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
}
