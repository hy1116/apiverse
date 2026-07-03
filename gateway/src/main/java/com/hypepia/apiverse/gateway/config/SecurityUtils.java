package com.hypepia.apiverse.gateway.config;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

// 인증이 필수인 엔드포인트는 컨트롤러 파라미터에 @AuthenticationPrincipal Mono<Long> principal을 쓴다.
// 이 클래스는 미인증 요청도 허용하되 로그인 상태면 userId를 쓰고 싶은 경우(예: 비로그인 시 {} 반환)에만 사용한다 —
// @AuthenticationPrincipal Mono<Long>은 인증 정보가 아예 없으면 null을 주입해 NPE가 나기 때문.
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Mono<Long> currentUserIdOrEmpty() {
        return ReactiveSecurityContextHolder.getContext()
                .mapNotNull(SecurityContext::getAuthentication)
                .mapNotNull(auth -> (Long) auth.getPrincipal());
    }
}
