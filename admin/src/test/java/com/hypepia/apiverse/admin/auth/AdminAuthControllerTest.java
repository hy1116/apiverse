package com.hypepia.apiverse.admin.auth;

import com.hypepia.apiverse.admin.config.JwtUtils;
import com.hypepia.apiverse.admin.config.TestSecurityConfig;
import com.hypepia.apiverse.core.entity.User;
import com.hypepia.apiverse.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.BDDMockito.given;

@WebFluxTest(controllers = AdminAuthController.class)
@Import(TestSecurityConfig.class)
class AdminAuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtUtils jwtUtils;

    @Test
    void login_admin_success_returns_user_and_token() {
        User admin = User.builder()
                .id(1L).email("admin@example.com").passwordHash("hashed")
                .companyName("ApiVerse").tier("FREE").role("ADMIN").build();

        given(userRepository.findByEmail("admin@example.com")).willReturn(Mono.just(admin));
        given(passwordEncoder.matches("pass123", "hashed")).willReturn(true);
        given(jwtUtils.generateToken(1L)).willReturn("mock-token");

        webTestClient.post().uri("/api/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "admin@example.com", "password", "pass123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo("admin@example.com")
                .jsonPath("$.role").isEqualTo("ADMIN")
                .jsonPath("$.token").isEqualTo("mock-token");
    }

    @Test
    void login_non_admin_returns_403() {
        User regular = User.builder()
                .id(2L).email("user@example.com").passwordHash("hashed").tier("FREE").build();

        given(userRepository.findByEmail("user@example.com")).willReturn(Mono.just(regular));
        given(passwordEncoder.matches("pass123", "hashed")).willReturn(true);

        webTestClient.post().uri("/api/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "user@example.com", "password", "pass123"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void login_wrong_password_returns_401() {
        User admin = User.builder()
                .id(1L).email("admin@example.com").passwordHash("hashed").role("ADMIN").build();

        given(userRepository.findByEmail("admin@example.com")).willReturn(Mono.just(admin));
        given(passwordEncoder.matches("wrong", "hashed")).willReturn(false);

        webTestClient.post().uri("/api/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "admin@example.com", "password", "wrong"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void login_unknown_user_returns_401() {
        given(userRepository.findByEmail("nobody@example.com")).willReturn(Mono.empty());

        webTestClient.post().uri("/api/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "nobody@example.com", "password", "pass"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void login_empty_email_returns_400() {
        webTestClient.post().uri("/api/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("password", "pass"))
                .exchange()
                .expectStatus().isBadRequest();
    }
}
