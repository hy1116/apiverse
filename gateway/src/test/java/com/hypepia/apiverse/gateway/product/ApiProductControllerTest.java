package com.hypepia.apiverse.gateway.product;

import com.hypepia.apiverse.core.entity.ApiKey;
import com.hypepia.apiverse.core.entity.ApiProduct;
import com.hypepia.apiverse.core.repository.ApiKeyRepository;
import com.hypepia.apiverse.core.repository.ApiProductRepository;
import com.hypepia.apiverse.core.repository.UserRepository;
import com.hypepia.apiverse.gateway.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

@WebFluxTest(controllers = ApiProductController.class)
@Import(TestSecurityConfig.class)
class ApiProductControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ApiProductRepository apiProductRepository;

    @MockitoBean
    private ApiKeyRepository apiKeyRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final ApiProduct WEATHER_API = ApiProduct.builder()
            .id(1L).name("기상청 날씨 API").description("날씨 데이터")
            .baseUrl("https://api.weather.go.kr/v1").isPremium(false).isActive(true).build();

    private static final ApiProduct STOCK_API = ApiProduct.builder()
            .id(2L).name("실시간 주식 시세 API").description("주식 데이터")
            .baseUrl("https://api.krx-data.co.kr/v1").isPremium(true).isActive(true).build();

    // ── GET /api/products ────────────────────────────────────────────────────

    @Test
    void listProducts_returns_all_active_products() {
        given(apiProductRepository.findAllByIsActiveTrueOrderByIsPremiumAsc())
                .willReturn(Flux.just(WEATHER_API, STOCK_API));

        webTestClient.get().uri("/api/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].name").isEqualTo("기상청 날씨 API");
    }

    @Test
    void listProducts_empty_returns_empty_array() {
        given(apiProductRepository.findAllByIsActiveTrueOrderByIsPremiumAsc())
                .willReturn(Flux.empty());

        webTestClient.get().uri("/api/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("[]");
    }

    // ── GET /api/products/{id} ───────────────────────────────────────────────

    @Test
    void getProduct_found_returns_product() {
        given(apiProductRepository.findById(1L)).willReturn(Mono.just(WEATHER_API));

        webTestClient.get().uri("/api/products/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("기상청 날씨 API");
    }

    @Test
    void getProduct_not_found_returns_404() {
        given(apiProductRepository.findById(99L)).willReturn(Mono.empty());

        webTestClient.get().uri("/api/products/99")
                .exchange()
                .expectStatus().isNotFound();
    }

    // ── GET /api/products/{id}/my-key ────────────────────────────────────────

    @Test
    void getMyKey_authenticated_with_key_returns_key_value() {
        ApiKey key = ApiKey.builder()
                .id(1L).userId(1L).apiProductId(1L)
                .apiKeyValue("apiverse_sandbox_abc123").build();

        given(apiKeyRepository.findByUserIdAndApiProductId(1L, 1L)).willReturn(Mono.just(key));

        webTestClient.mutateWith(mockAuthentication(
                        new UsernamePasswordAuthenticationToken(1L, null, List.of())))
                .get().uri("/api/products/1/my-key")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.apiKeyValue").isEqualTo("apiverse_sandbox_abc123");
    }

    @Test
    void getMyKey_authenticated_without_key_returns_empty_map() {
        given(apiKeyRepository.findByUserIdAndApiProductId(1L, 1L)).willReturn(Mono.empty());

        webTestClient.mutateWith(mockAuthentication(
                        new UsernamePasswordAuthenticationToken(1L, null, List.of())))
                .get().uri("/api/products/1/my-key")
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("{}");
    }

    @Test
    void getMyKey_unauthenticated_returns_empty_map() {
        webTestClient.get().uri("/api/products/1/my-key")
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("{}");
    }
}
