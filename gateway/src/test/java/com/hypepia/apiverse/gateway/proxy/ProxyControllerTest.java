package com.hypepia.apiverse.gateway.proxy;

import com.hypepia.apiverse.gateway.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@WebFluxTest(controllers = ProxyController.class)
@Import(TestSecurityConfig.class)
class ProxyControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ProxyService proxyService;

    @Test
    void proxy_delegates_to_service_and_returns_200() {
        given(proxyService.proxy(any(), eq("weather-api")))
                .willReturn(Mono.just(ResponseEntity.ok("OK")));

        webTestClient.get().uri("/gateway/weather-api/some/path")
                .header("X-API-KEY", "apiverse_sandbox_test")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("OK");
    }

    @Test
    void proxy_missing_api_key_returns_401() {
        given(proxyService.proxy(any(), eq("weather-api")))
                .willReturn(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("X-API-KEY header is required")));

        webTestClient.get().uri("/gateway/weather-api/some/path")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void proxy_rate_limited_returns_429() {
        given(proxyService.proxy(any(), eq("weather-api")))
                .willReturn(Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body("Rate limit exceeded")));

        webTestClient.get().uri("/gateway/weather-api/some/path")
                .header("X-API-KEY", "apiverse_sandbox_test")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void proxy_upstream_unavailable_returns_502() {
        given(proxyService.proxy(any(), eq("weather-api")))
                .willReturn(Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body("Upstream unavailable")));

        webTestClient.get().uri("/gateway/weather-api/some/path")
                .header("X-API-KEY", "apiverse_sandbox_test")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY);
    }
}
