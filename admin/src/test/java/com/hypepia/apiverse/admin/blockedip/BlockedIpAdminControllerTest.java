package com.hypepia.apiverse.admin.blockedip;

import com.hypepia.apiverse.admin.config.TestSecurityConfig;
import com.hypepia.apiverse.core.entity.BlockedIp;
import com.hypepia.apiverse.core.entity.User;
import com.hypepia.apiverse.core.repository.BlockedIpRepository;
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

@WebFluxTest(controllers = BlockedIpAdminController.class)
@Import(TestSecurityConfig.class)
class BlockedIpAdminControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private BlockedIpRepository blockedIpRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final User ADMIN = User.builder().id(1L).role("ADMIN").build();
    private static final User REGULAR = User.builder().id(2L).tier("FREE").build();

    private WebTestClient asUser(long userId) {
        return webTestClient.mutateWith(mockAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of())));
    }

    @Test
    void list_admin_returns_all_blocked_ips() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(blockedIpRepository.findAllByOrderByIdDesc()).willReturn(Flux.just(
                BlockedIp.builder().id(1L).ipAddress("1.2.3.4").reason("abuse").build()
        ));

        asUser(1L).get().uri("/api/admin/blocked-ips")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].ipAddress").isEqualTo("1.2.3.4");
    }

    @Test
    void list_non_admin_returns_403() {
        given(userRepository.findById(2L)).willReturn(Mono.just(REGULAR));

        asUser(2L).get().uri("/api/admin/blocked-ips")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void add_new_ip_returns_201() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(blockedIpRepository.findByIpAddress("9.9.9.9")).willReturn(Mono.empty());
        given(blockedIpRepository.save(any(BlockedIp.class))).willReturn(
                Mono.just(BlockedIp.builder().id(2L).ipAddress("9.9.9.9").reason("spam").build()));

        asUser(1L).post().uri("/api/admin/blocked-ips")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("ipAddress", "9.9.9.9", "reason", "spam"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.ipAddress").isEqualTo("9.9.9.9");
    }

    @Test
    void add_duplicate_ip_returns_409() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(blockedIpRepository.findByIpAddress("1.2.3.4")).willReturn(
                Mono.just(BlockedIp.builder().id(1L).ipAddress("1.2.3.4").build()));

        asUser(1L).post().uri("/api/admin/blocked-ips")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("ipAddress", "1.2.3.4"))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void remove_admin_returns_204() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(blockedIpRepository.deleteById(1L)).willReturn(Mono.empty());

        asUser(1L).delete().uri("/api/admin/blocked-ips/1")
                .exchange()
                .expectStatus().isNoContent();
    }
}
