package com.hypepia.apiverse.admin.user;

import com.hypepia.apiverse.admin.config.TestSecurityConfig;
import com.hypepia.apiverse.core.entity.User;
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

@WebFluxTest(controllers = UserAdminController.class)
@Import(TestSecurityConfig.class)
class UserAdminControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UserRepository userRepository;

    private static final User ADMIN = User.builder().id(1L).email("admin@example.com").role("ADMIN").build();
    private static final User REGULAR = User.builder().id(2L).email("user@example.com").tier("FREE").build();
    private static final User TARGET = User.builder()
            .id(5L).email("target@example.com").companyName("TargetCo").tier("FREE").build();

    private WebTestClient asUser(long userId) {
        return webTestClient.mutateWith(mockAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of())));
    }

    // ── GET /api/admin/users ─────────────────────────────────────────────────

    @Test
    void list_admin_returns_users_without_password_hash() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(userRepository.findAllByOrderByIdDesc()).willReturn(Flux.just(TARGET));

        asUser(1L).get().uri("/api/admin/users")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].email").isEqualTo("target@example.com")
                .jsonPath("$[0].passwordHash").doesNotExist();
    }

    @Test
    void list_non_admin_returns_403() {
        given(userRepository.findById(2L)).willReturn(Mono.just(REGULAR));

        asUser(2L).get().uri("/api/admin/users")
                .exchange()
                .expectStatus().isForbidden();
    }

    // ── GET /api/admin/users/{id} ─────────────────────────────────────────────

    @Test
    void detail_admin_returns_user() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(userRepository.findById(5L)).willReturn(Mono.just(TARGET));

        asUser(1L).get().uri("/api/admin/users/5")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.email").isEqualTo("target@example.com");
    }

    @Test
    void detail_not_found_returns_404() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(userRepository.findById(99L)).willReturn(Mono.empty());

        asUser(1L).get().uri("/api/admin/users/99")
                .exchange()
                .expectStatus().isNotFound();
    }

    // ── PATCH /api/admin/users/{id}/tier ──────────────────────────────────────

    @Test
    void updateTier_admin_succeeds() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(userRepository.findById(5L)).willReturn(Mono.just(TARGET));
        given(userRepository.save(any())).willAnswer(inv -> Mono.just(inv.getArgument(0)));

        asUser(1L).patch().uri("/api/admin/users/5/tier")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("tier", "PREMIUM"))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.tier").isEqualTo("PREMIUM");
    }

    @Test
    void updateTier_non_admin_returns_403() {
        given(userRepository.findById(2L)).willReturn(Mono.just(REGULAR));

        asUser(2L).patch().uri("/api/admin/users/5/tier")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("tier", "PREMIUM"))
                .exchange()
                .expectStatus().isForbidden();
    }
}
