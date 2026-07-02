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

    // ── GET /api/admin/keys/{id} ─────────────────────────────────────────────

    @Test
    void detail_admin_returns_key_with_owner_and_product() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiKeyRepository.findById(10L)).willReturn(Mono.just(KEY));
        given(apiProductRepository.findById(1L)).willReturn(Mono.just(PRODUCT));
        given(userRepository.findById(5L)).willReturn(Mono.just(KEY_OWNER));

        asUser(1L).get().uri("/api/admin/keys/10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.apiKeyValue").isEqualTo("apiverse_sandbox_abc123")
                .jsonPath("$.userEmail").isEqualTo("owner@example.com");
    }

    @Test
    void detail_not_found_returns_404() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiKeyRepository.findById(99L)).willReturn(Mono.empty());

        asUser(1L).get().uri("/api/admin/keys/99")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void detail_non_admin_returns_403() {
        given(userRepository.findById(2L)).willReturn(Mono.just(REGULAR));

        asUser(2L).get().uri("/api/admin/keys/10")
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

    // ── PATCH /api/admin/keys/{id}/whitelist-ip ──────────────────────────────

    @Test
    void updateWhiteListIp_admin_sets_value() {
        ApiKey key = ApiKey.builder().id(11L).userId(5L).apiProductId(1L)
                .apiKeyValue("apiverse_sandbox_xyz").isActive(true).build();

        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiKeyRepository.findById(11L)).willReturn(Mono.just(key));
        given(apiKeyRepository.save(key)).willReturn(Mono.just(key));
        given(apiProductRepository.findById(1L)).willReturn(Mono.just(PRODUCT));
        given(userRepository.findById(5L)).willReturn(Mono.just(KEY_OWNER));

        asUser(1L).patch().uri("/api/admin/keys/11/whitelist-ip")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(java.util.Map.of("whiteListIp", "1.2.3.4,5.6.7.8"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.whiteListIp").isEqualTo("1.2.3.4,5.6.7.8");
    }

    @Test
    void updateWhiteListIp_blank_clears_restriction() {
        ApiKey key = ApiKey.builder().id(12L).userId(5L).apiProductId(1L)
                .apiKeyValue("apiverse_sandbox_xyz").isActive(true).whiteListIp("1.2.3.4").build();

        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiKeyRepository.findById(12L)).willReturn(Mono.just(key));
        given(apiKeyRepository.save(key)).willReturn(Mono.just(key));
        given(apiProductRepository.findById(1L)).willReturn(Mono.just(PRODUCT));
        given(userRepository.findById(5L)).willReturn(Mono.just(KEY_OWNER));

        asUser(1L).patch().uri("/api/admin/keys/12/whitelist-ip")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(java.util.Map.of("whiteListIp", ""))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.whiteListIp").doesNotExist();
    }

    @Test
    void updateWhiteListIp_not_found_returns_404() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiKeyRepository.findById(99L)).willReturn(Mono.empty());

        asUser(1L).patch().uri("/api/admin/keys/99/whitelist-ip")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(java.util.Map.of("whiteListIp", "1.2.3.4"))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void updateWhiteListIp_non_admin_returns_403() {
        given(userRepository.findById(2L)).willReturn(Mono.just(REGULAR));

        asUser(2L).patch().uri("/api/admin/keys/10/whitelist-ip")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(java.util.Map.of("whiteListIp", "1.2.3.4"))
                .exchange()
                .expectStatus().isForbidden();
    }

    // ── PATCH /api/admin/keys/{id}/quota ─────────────────────────────────────

    @Test
    void updateQuota_admin_sets_limited_value() {
        ApiKey key = ApiKey.builder().id(13L).userId(5L).apiProductId(1L)
                .apiKeyValue("apiverse_sandbox_xyz").isActive(true).monthlyQuota(-1).build();

        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiKeyRepository.findById(13L)).willReturn(Mono.just(key));
        given(apiKeyRepository.save(key)).willReturn(Mono.just(key));
        given(apiProductRepository.findById(1L)).willReturn(Mono.just(PRODUCT));
        given(userRepository.findById(5L)).willReturn(Mono.just(KEY_OWNER));

        asUser(1L).patch().uri("/api/admin/keys/13/quota")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(java.util.Map.of("monthlyQuota", 1000))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.monthlyQuota").isEqualTo(1000);
    }

    @Test
    void updateQuota_minus_one_sets_unlimited() {
        ApiKey key = ApiKey.builder().id(14L).userId(5L).apiProductId(1L)
                .apiKeyValue("apiverse_sandbox_xyz").isActive(true).monthlyQuota(500).build();

        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiKeyRepository.findById(14L)).willReturn(Mono.just(key));
        given(apiKeyRepository.save(key)).willReturn(Mono.just(key));
        given(apiProductRepository.findById(1L)).willReturn(Mono.just(PRODUCT));
        given(userRepository.findById(5L)).willReturn(Mono.just(KEY_OWNER));

        asUser(1L).patch().uri("/api/admin/keys/14/quota")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(java.util.Map.of("monthlyQuota", -1))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.monthlyQuota").isEqualTo(-1);
    }

    @Test
    void updateQuota_negative_other_than_minus_one_returns_400() {
        asUser(1L).patch().uri("/api/admin/keys/13/quota")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(java.util.Map.of("monthlyQuota", -5))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updateQuota_not_found_returns_404() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiKeyRepository.findById(99L)).willReturn(Mono.empty());

        asUser(1L).patch().uri("/api/admin/keys/99/quota")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(java.util.Map.of("monthlyQuota", 1000))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void updateQuota_non_admin_returns_403() {
        given(userRepository.findById(2L)).willReturn(Mono.just(REGULAR));

        asUser(2L).patch().uri("/api/admin/keys/10/quota")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(java.util.Map.of("monthlyQuota", 1000))
                .exchange()
                .expectStatus().isForbidden();
    }
}
