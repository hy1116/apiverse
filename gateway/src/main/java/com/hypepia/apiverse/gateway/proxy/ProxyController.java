package com.hypepia.apiverse.gateway.proxy;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
public class ProxyController {

    private final ProxyService proxyService;

    @RequestMapping("/{productId}/**")
    public Mono<ResponseEntity<String>> proxy(ServerWebExchange exchange,
                                              @PathVariable Long productId) {
        return proxyService.proxy(exchange, productId);
    }
}
