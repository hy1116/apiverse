package com.hypepia.apiverse.admin.product;

import com.hypepia.apiverse.admin.config.TestSecurityConfig;
import com.hypepia.apiverse.core.entity.ApiProduct;
import com.hypepia.apiverse.core.entity.User;
import com.hypepia.apiverse.core.repository.ApiProductRepository;
import com.hypepia.apiverse.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

@WebFluxTest(controllers = ProductAdminController.class)
@Import(TestSecurityConfig.class)
class ProductAdminControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ApiProductRepository apiProductRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final User ADMIN = User.builder().id(1L).role("ADMIN").build();
    private static final User REGULAR = User.builder().id(2L).tier("FREE").build();

    private static final ApiProduct PENDING_PRODUCT = ApiProduct.builder()
            .id(1L).name("신규 API").isActive(false).build();

    private static final ApiProduct ACTIVE_PRODUCT = ApiProduct.builder()
            .id(2L).name("승인된 API").isActive(true).build();

    private WebTestClient asUser(long userId) {
        return webTestClient.mutateWith(mockAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of())));
    }

    // ── GET /api/admin/products ──────────────────────────────────────────────

    @Test
    void listAll_admin_returns_all_products() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiProductRepository.findAllByOrderByIdDesc())
                .willReturn(Flux.just(ACTIVE_PRODUCT, PENDING_PRODUCT));

        asUser(1L).get().uri("/api/admin/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);
    }

    @Test
    void listAll_non_admin_returns_403() {
        given(userRepository.findById(2L)).willReturn(Mono.just(REGULAR));

        asUser(2L).get().uri("/api/admin/products")
                .exchange()
                .expectStatus().isForbidden();
    }

    // ── GET /api/admin/products/pending ──────────────────────────────────────

    @Test
    void listPending_admin_returns_pending_products() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiProductRepository.findAllByIsActiveFalseOrderByIdDesc())
                .willReturn(Flux.just(PENDING_PRODUCT));

        asUser(1L).get().uri("/api/admin/products/pending")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].name").isEqualTo("신규 API");
    }

    // ── GET /api/admin/products/{id} ─────────────────────────────────────────

    @Test
    void detail_admin_returns_product_with_upstream_fields() {
        ApiProduct withUpstream = ACTIVE_PRODUCT.toBuilder()
                .code("weather-api").upstreamApiKey("secret-key").upstreamKeyParam("query:serviceKey").build();

        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiProductRepository.findById(2L)).willReturn(Mono.just(withUpstream));

        asUser(1L).get().uri("/api/admin/products/2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo("weather-api")
                .jsonPath("$.upstreamApiKey").isEqualTo("secret-key")
                .jsonPath("$.upstreamKeyParam").isEqualTo("query:serviceKey");
    }

    @Test
    void detail_not_found_returns_404() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiProductRepository.findById(99L)).willReturn(Mono.empty());

        asUser(1L).get().uri("/api/admin/products/99")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void detail_non_admin_returns_403() {
        given(userRepository.findById(2L)).willReturn(Mono.just(REGULAR));

        asUser(2L).get().uri("/api/admin/products/2")
                .exchange()
                .expectStatus().isForbidden();
    }

    // ── PATCH /api/admin/products/{id}/approve ───────────────────────────────

    @Test
    void approve_admin_succeeds() {
        ApiProduct approved = PENDING_PRODUCT.toBuilder().isActive(true).build();

        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiProductRepository.findById(1L)).willReturn(Mono.just(PENDING_PRODUCT));
        given(apiProductRepository.save(any())).willReturn(Mono.just(approved));

        asUser(1L).patch().uri("/api/admin/products/1/approve")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.isActive").isEqualTo(true);
    }

    @Test
    void approve_non_admin_returns_403() {
        given(userRepository.findById(2L)).willReturn(Mono.just(REGULAR));

        asUser(2L).patch().uri("/api/admin/products/1/approve")
                .exchange()
                .expectStatus().isForbidden();
    }

    // ── DELETE /api/admin/products/{id}/reject ───────────────────────────────

    @Test
    void reject_pending_product_returns_204() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiProductRepository.findById(1L)).willReturn(Mono.just(PENDING_PRODUCT));
        given(apiProductRepository.deleteById(1L)).willReturn(Mono.empty());

        asUser(1L).delete().uri("/api/admin/products/1/reject")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void reject_active_product_returns_409() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiProductRepository.findById(2L)).willReturn(Mono.just(ACTIVE_PRODUCT));

        asUser(1L).delete().uri("/api/admin/products/2/reject")
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    // ── PATCH /api/admin/products/{id} ───────────────────────────────────────

    @Test
    void update_admin_succeeds() {
        ApiProduct updated = ACTIVE_PRODUCT.toBuilder().description("수정된 설명").build();

        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiProductRepository.findById(2L)).willReturn(Mono.just(ACTIVE_PRODUCT));
        given(apiProductRepository.save(any())).willReturn(Mono.just(updated));

        asUser(1L).patch().uri("/api/admin/products/2")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("description", "수정된 설명"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.description").isEqualTo("수정된 설명");
    }

    @Test
    void update_admin_can_set_code_and_upstream_key() {
        ApiProduct updated = ACTIVE_PRODUCT.toBuilder()
                .code("weather-api").upstreamApiKey("secret-key").upstreamKeyParam("query:serviceKey").build();

        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiProductRepository.findById(2L)).willReturn(Mono.just(ACTIVE_PRODUCT));
        given(apiProductRepository.save(any())).willReturn(Mono.just(updated));

        asUser(1L).patch().uri("/api/admin/products/2")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("code", "weather-api", "upstreamApiKey", "secret-key", "upstreamKeyParam", "query:serviceKey"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo("weather-api")
                .jsonPath("$.upstreamApiKey").isEqualTo("secret-key")
                .jsonPath("$.upstreamKeyParam").isEqualTo("query:serviceKey");
    }
}
