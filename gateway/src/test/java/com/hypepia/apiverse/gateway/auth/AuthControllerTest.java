package com.hypepia.apiverse.gateway.auth;

import com.hypepia.apiverse.core.entity.User;
import com.hypepia.apiverse.core.repository.UserRepository;
import com.hypepia.apiverse.gateway.config.JwtUtils;
import com.hypepia.apiverse.gateway.config.TestSecurityConfig;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@WebFluxTest(controllers = AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtUtils jwtUtils;

    // ── signup ──────────────────────────────────────────────────────────────

    @Test
    void signup_success_returns_user_and_token() {
        User saved = User.builder()
                .id(1L).email("test@example.com").companyName("TestCo").tier("FREE").build();

        given(userRepository.findByEmail("test@example.com")).willReturn(Mono.empty());
        given(passwordEncoder.encode(any())).willReturn("hashed");
        given(userRepository.save(any())).willReturn(Mono.just(saved));
        given(jwtUtils.generateToken(1L)).willReturn("mock-token");

        webTestClient.post().uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "test@example.com", "password", "pass123", "companyName", "TestCo"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo("test@example.com")
                .jsonPath("$.token").isEqualTo("mock-token")
                .jsonPath("$.tier").isEqualTo("FREE");
    }

    @Test
    void signup_duplicate_email_returns_409() {
        given(userRepository.findByEmail("dup@example.com"))
                .willReturn(Mono.just(User.builder().email("dup@example.com").build()));

        webTestClient.post().uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "dup@example.com", "password", "pass"))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void signup_empty_email_returns_400() {
        webTestClient.post().uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("password", "pass"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void signup_empty_password_returns_400() {
        webTestClient.post().uri("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "test@example.com"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ── login ───────────────────────────────────────────────────────────────

    @Test
    void login_success_returns_user_and_token() {
        User user = User.builder()
                .id(1L).email("test@example.com").passwordHash("hashed")
                .companyName("TestCo").tier("FREE").build();

        given(userRepository.findByEmail("test@example.com")).willReturn(Mono.just(user));
        given(passwordEncoder.matches("pass123", "hashed")).willReturn(true);
        given(jwtUtils.generateToken(1L)).willReturn("mock-token");

        webTestClient.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "test@example.com", "password", "pass123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo("test@example.com")
                .jsonPath("$.token").isEqualTo("mock-token");
    }

    @Test
    void login_wrong_password_returns_401() {
        User user = User.builder()
                .id(1L).email("test@example.com").passwordHash("hashed").build();

        given(userRepository.findByEmail("test@example.com")).willReturn(Mono.just(user));
        given(passwordEncoder.matches("wrong", "hashed")).willReturn(false);

        webTestClient.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "test@example.com", "password", "wrong"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void login_unknown_user_returns_401() {
        given(userRepository.findByEmail("nobody@example.com")).willReturn(Mono.empty());

        webTestClient.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("email", "nobody@example.com", "password", "pass"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void login_empty_email_returns_400() {
        webTestClient.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("password", "pass"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ── check-email ─────────────────────────────────────────────────────────

    @Test
    void checkEmail_new_email_returns_available_true() {
        given(userRepository.findByEmail("new@example.com")).willReturn(Mono.empty());

        webTestClient.get().uri("/api/auth/check-email?email=new@example.com")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.available").isEqualTo(true);
    }

    @Test
    void checkEmail_existing_email_returns_available_false() {
        given(userRepository.findByEmail("taken@example.com"))
                .willReturn(Mono.just(User.builder().email("taken@example.com").build()));

        webTestClient.get().uri("/api/auth/check-email?email=taken@example.com")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.available").isEqualTo(false);
    }
}
