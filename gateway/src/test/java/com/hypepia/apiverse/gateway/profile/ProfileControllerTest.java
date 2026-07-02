package com.hypepia.apiverse.gateway.profile;

import com.hypepia.apiverse.core.entity.User;
import com.hypepia.apiverse.core.repository.UserRepository;
import com.hypepia.apiverse.gateway.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

@WebFluxTest(controllers = ProfileController.class)
@Import(TestSecurityConfig.class)
class ProfileControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private UserRepository userRepository;

    private WebTestClient asUser(long userId) {
        return webTestClient.mutateWith(mockAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of())));
    }

    @Test
    void me_returns_profile_without_password_hash() {
        User user = User.builder()
                .id(1L).email("dev@hypepia.com").passwordHash("secret-hash")
                .companyName("Hypepia Inc.").phone("010-1234-5678").tier("FREE").build();
        given(userRepository.findById(1L)).willReturn(Mono.just(user));

        asUser(1L).get().uri("/api/profile")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo("dev@hypepia.com")
                .jsonPath("$.companyName").isEqualTo("Hypepia Inc.")
                .jsonPath("$.phone").isEqualTo("010-1234-5678")
                .jsonPath("$.passwordHash").doesNotExist();
    }

    @Test
    void update_changes_companyName_and_phone() {
        User user = User.builder()
                .id(1L).email("dev@hypepia.com").companyName("Old Inc.").phone("010-0000-0000").tier("FREE").build();
        given(userRepository.findById(1L)).willReturn(Mono.just(user));
        given(userRepository.save(any(User.class))).willReturn(Mono.just(user));

        asUser(1L).patch().uri("/api/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("companyName", "New Inc.", "phone", "010-9999-9999"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.companyName").isEqualTo("New Inc.")
                .jsonPath("$.phone").isEqualTo("010-9999-9999");
    }

    @Test
    void update_blank_values_clear_fields() {
        User user = User.builder()
                .id(1L).email("dev@hypepia.com").companyName("Old Inc.").phone("010-0000-0000").tier("FREE").build();
        given(userRepository.findById(1L)).willReturn(Mono.just(user));
        given(userRepository.save(any(User.class))).willReturn(Mono.just(user));

        asUser(1L).patch().uri("/api/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("companyName", "", "phone", ""))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.companyName").doesNotExist()
                .jsonPath("$.phone").doesNotExist();
    }
}
