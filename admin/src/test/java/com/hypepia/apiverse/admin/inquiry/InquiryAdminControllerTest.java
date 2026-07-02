package com.hypepia.apiverse.admin.inquiry;

import com.hypepia.apiverse.admin.config.TestSecurityConfig;
import com.hypepia.apiverse.core.entity.Inquiry;
import com.hypepia.apiverse.core.entity.User;
import com.hypepia.apiverse.core.repository.InquiryRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

@WebFluxTest(controllers = InquiryAdminController.class)
@Import(TestSecurityConfig.class)
class InquiryAdminControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private InquiryRepository inquiryRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final User ADMIN = User.builder().id(1L).role("ADMIN").build();
    private static final User REGULAR = User.builder().id(2L).tier("FREE").build();

    private static final Inquiry INQUIRY_1 = Inquiry.builder()
            .id(1L).userId(5L).title("테스트 문의").content("문의 내용입니다")
            .status("PENDING").createdAt(LocalDateTime.now()).build();

    private WebTestClient asUser(long userId) {
        return webTestClient.mutateWith(mockAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of())));
    }

    // ── GET /api/admin/inquiries ─────────────────────────────────────────────

    @Test
    void list_admin_returns_all_inquiries() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(inquiryRepository.findAllByOrderByCreatedAtDesc()).willReturn(Flux.just(INQUIRY_1));

        asUser(1L).get().uri("/api/admin/inquiries")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].title").isEqualTo("테스트 문의");
    }

    @Test
    void list_non_admin_returns_403() {
        given(userRepository.findById(2L)).willReturn(Mono.just(REGULAR));

        asUser(2L).get().uri("/api/admin/inquiries")
                .exchange()
                .expectStatus().isForbidden();
    }

    // ── GET /api/admin/inquiries/{id} ────────────────────────────────────────

    @Test
    void detail_admin_returns_inquiry() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(inquiryRepository.findById(1L)).willReturn(Mono.just(INQUIRY_1));

        asUser(1L).get().uri("/api/admin/inquiries/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.id").isEqualTo(1);
    }

    @Test
    void detail_not_found_returns_404() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(inquiryRepository.findById(99L)).willReturn(Mono.empty());

        asUser(1L).get().uri("/api/admin/inquiries/99")
                .exchange()
                .expectStatus().isNotFound();
    }

    // ── POST /api/admin/inquiries/{id}/answer ────────────────────────────────

    @Test
    void answer_admin_succeeds() {
        Inquiry answered = INQUIRY_1.toBuilder().answer("답변입니다").status("ANSWERED").build();

        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(inquiryRepository.findById(1L)).willReturn(Mono.just(INQUIRY_1));
        given(inquiryRepository.save(any())).willReturn(Mono.just(answered));

        asUser(1L).post().uri("/api/admin/inquiries/1/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("answer", "답변입니다"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ANSWERED")
                .jsonPath("$.answer").isEqualTo("답변입니다");
    }

    @Test
    void answer_non_admin_returns_403() {
        given(userRepository.findById(2L)).willReturn(Mono.just(REGULAR));

        asUser(2L).post().uri("/api/admin/inquiries/1/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("answer", "무단 답변 시도"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void answer_inquiry_not_found_returns_404() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(inquiryRepository.findById(99L)).willReturn(Mono.empty());

        asUser(1L).post().uri("/api/admin/inquiries/99/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("answer", "답변"))
                .exchange()
                .expectStatus().isNotFound();
    }
}
