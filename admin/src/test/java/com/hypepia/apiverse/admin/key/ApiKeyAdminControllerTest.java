package com.hypepia.apiverse.admin.key;

import com.hypepia.apiverse.admin.config.TestSecurityConfig;
import com.hypepia.apiverse.core.entity.ApiKey;
import com.hypepia.apiverse.core.entity.ApiProduct;
import com.hypepia.apiverse.core.entity.User;
import com.hypepia.apiverse.core.repository.ApiKeyRepository;
import com.hypepia.apiverse.core.repository.ApiProductRepository;
import com.hypepia.apiverse.core.repository.UserRepository;
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

@WebFluxTest(controllers = ApiKeyAdminController.class)
@Import(TestSecurityConfig.class)
class ApiKeyAdminControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ApiKeyRepository apiKeyRepository;

    @MockitoBean
    private ApiProductRepository apiProductRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final User ADMIN = User.builder().id(1L).role("ADMIN").build();
    private static final User REGULAR = User.builder().id(2L).tier("FREE").build();

    private static final ApiProduct PRODUCT = ApiProduct.builder().id(1L).name("기상청 날씨 API").build();
    private static final User KEY_OWNER = User.builder().id(5L).email("owner@example.com").build();

    private static final ApiKey KEY = ApiKey.builder()
            .id(10L).userId(5L).apiProductId(1L)
            .apiKeyValue("apiverse_sandbox_abc123")
            .isActive(true).build();

    private WebTestClient asUser(long userId) {
        return webTestClient.mutateWith(mockAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of())));
    }

    // ── GET /api/admin/keys ───────────────────────────────────────────────────

    @Test
    void list_admin_returns_all_keys_with_owner_and_product() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiKeyRepository.findAllByOrderByIdDesc()).willReturn(Flux.just(KEY));
        given(apiProductRepository.findById(1L)).willReturn(Mono.just(PRODUCT));
        given(userRepository.findById(5L)).willReturn(Mono.just(KEY_OWNER));

        asUser(1L).get().uri("/api/admin/keys")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].apiKeyValue").isEqualTo("apiverse_sandbox_abc123")
                .jsonPath("$[0].apiProductName").isEqualTo("기상청 날씨 API")
                .jsonPath("$[0].userEmail").isEqualTo("owner@example.com");
    }

    @Test
    void list_non_admin_returns_403() {
        given(userRepository.findById(2L)).willReturn(Mono.just(REGULAR));

        asUser(2L).get().uri("/api/admin/keys")
                .exchange()
                .expectStatus().isForbidden();
    }

    // ── DELETE /api/admin/keys/{id} ──────────────────────────────────────────

    @Test
    void revoke_admin_succeeds_regardless_of_owner() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiKeyRepository.findById(10L)).willReturn(Mono.just(KEY));
        given(apiKeyRepository.deleteById(10L)).willReturn(Mono.empty());

        asUser(1L).delete().uri("/api/admin/keys/10")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void revoke_not_found_returns_404() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiKeyRepository.findById(99L)).willReturn(Mono.empty());

        asUser(1L).delete().uri("/api/admin/keys/99")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void revoke_non_admin_returns_403() {
        given(userRepository.findById(2L)).willReturn(Mono.just(REGULAR));

        asUser(2L).delete().uri("/api/admin/keys/10")
                .exchange()
                .expectStatus().isForbidden();
    }
}
