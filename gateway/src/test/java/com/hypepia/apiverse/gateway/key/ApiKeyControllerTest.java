package com.hypepia.apiverse.gateway.key;

import com.hypepia.apiverse.core.entity.ApiKey;
import com.hypepia.apiverse.core.entity.ApiProduct;
import com.hypepia.apiverse.core.repository.ApiKeyRepository;
import com.hypepia.apiverse.core.repository.ApiProductRepository;
import com.hypepia.apiverse.gateway.config.TestSecurityConfig;
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

@WebFluxTest(controllers = ApiKeyController.class)
@Import(TestSecurityConfig.class)
class ApiKeyControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ApiKeyRepository apiKeyRepository;

    @MockitoBean
    private ApiProductRepository apiProductRepository;

    private static final long USER_ID = 1L;
    private static final UsernamePasswordAuthenticationToken AUTH =
            new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());

    private final ApiProduct product = ApiProduct.builder()
            .id(1L).name("기상청 날씨 API").build();

    private final ApiKey activeKey = ApiKey.builder()
            .id(10L).userId(USER_ID).apiProductId(1L)
            .apiKeyValue("apiverse_sandbox_abc123")
            .monthlyQuota(-1).usedQuota(0).isActive(true).build();

    // ── GET /api/keys ────────────────────────────────────────────────────────

    @Test
    void listKeys_returns_active_keys_with_product_name() {
        given(apiKeyRepository.findByUserId(USER_ID)).willReturn(Flux.just(activeKey));
        given(apiProductRepository.findById(1L)).willReturn(Mono.just(product));

        webTestClient.mutateWith(mockAuthentication(AUTH))
                .get().uri("/api/keys")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].apiKeyValue").isEqualTo("apiverse_sandbox_abc123")
                .jsonPath("$[0].apiProductName").isEqualTo("기상청 날씨 API");
    }

    @Test
    void listKeys_filters_out_inactive_keys() {
        ApiKey inactiveKey = ApiKey.builder()
                .id(11L).userId(USER_ID).apiProductId(1L)
                .apiKeyValue("apiverse_sandbox_inactive")
                .isActive(false).build();

        given(apiKeyRepository.findByUserId(USER_ID)).willReturn(Flux.just(inactiveKey));

        webTestClient.mutateWith(mockAuthentication(AUTH))
                .get().uri("/api/keys")
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("[]");
    }

    // ── POST /api/keys ───────────────────────────────────────────────────────

    @Test
    void issueKey_success_returns_new_key() {
        given(apiKeyRepository.findByUserIdAndApiProductId(USER_ID, 1L)).willReturn(Mono.empty());
        given(apiKeyRepository.save(any())).willReturn(Mono.just(activeKey));
        given(apiProductRepository.findById(1L)).willReturn(Mono.just(product));

        webTestClient.mutateWith(mockAuthentication(AUTH))
                .post().uri("/api/keys")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("apiProductId", 1))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.apiKeyValue").isEqualTo("apiverse_sandbox_abc123")
                .jsonPath("$.apiProductName").isEqualTo("기상청 날씨 API");
    }

    @Test
    void issueKey_already_exists_returns_409() {
        given(apiKeyRepository.findByUserIdAndApiProductId(USER_ID, 1L))
                .willReturn(Mono.just(activeKey));

        webTestClient.mutateWith(mockAuthentication(AUTH))
                .post().uri("/api/keys")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("apiProductId", 1))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    // ── DELETE /api/keys/{id} ────────────────────────────────────────────────

    @Test
    void revokeKey_success_returns_204() {
        given(apiKeyRepository.findById(10L)).willReturn(Mono.just(activeKey));
        given(apiKeyRepository.deleteById(10L)).willReturn(Mono.empty());

        webTestClient.mutateWith(mockAuthentication(AUTH))
                .delete().uri("/api/keys/10")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void revokeKey_not_found_returns_404() {
        given(apiKeyRepository.findById(99L)).willReturn(Mono.empty());

        webTestClient.mutateWith(mockAuthentication(AUTH))
                .delete().uri("/api/keys/99")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void revokeKey_other_users_key_returns_403() {
        ApiKey otherUsersKey = ApiKey.builder()
                .id(10L).userId(999L).apiProductId(1L)
                .apiKeyValue("apiverse_sandbox_other").isActive(true).build();

        given(apiKeyRepository.findById(10L)).willReturn(Mono.just(otherUsersKey));

        webTestClient.mutateWith(mockAuthentication(AUTH))
                .delete().uri("/api/keys/10")
                .exchange()
                .expectStatus().isForbidden();
    }
}
