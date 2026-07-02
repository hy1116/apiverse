package com.hypepia.apiverse.gateway.inquiry;

import com.hypepia.apiverse.core.entity.Inquiry;
import com.hypepia.apiverse.core.entity.User;
import com.hypepia.apiverse.core.repository.InquiryRepository;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

@WebFluxTest(controllers = InquiryController.class)
@Import(TestSecurityConfig.class)
class InquiryControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private InquiryRepository inquiryRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final Inquiry INQUIRY_1 = Inquiry.builder()
            .id(1L).userId(1L).title("테스트 문의").content("문의 내용입니다")
            .status("PENDING").createdAt(LocalDateTime.now()).build();

    private WebTestClient asUser(long userId) {
        return webTestClient.mutateWith(mockAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of())));
    }

    // ── POST /api/inquiries ──────────────────────────────────────────────────

    @Test
    void submit_success_returns_saved_inquiry() {
        given(inquiryRepository.save(any())).willReturn(Mono.just(INQUIRY_1));

        asUser(1L).post().uri("/api/inquiries")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("title", "테스트 문의", "content", "문의 내용입니다"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.title").isEqualTo("테스트 문의")
                .jsonPath("$.status").isEqualTo("PENDING");
    }

    @Test
    void submit_missing_title_returns_400() {
        asUser(1L).post().uri("/api/inquiries")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("content", "내용만 있음"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void submit_missing_content_returns_400() {
        asUser(1L).post().uri("/api/inquiries")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("title", "제목만 있음"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ── GET /api/inquiries ───────────────────────────────────────────────────

    @Test
    void list_returns_user_inquiries() {
        given(inquiryRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .willReturn(Flux.just(INQUIRY_1));

        asUser(1L).get().uri("/api/inquiries")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].title").isEqualTo("테스트 문의");
    }

    @Test
    void list_returns_empty_when_no_inquiries() {
        given(inquiryRepository.findByUserIdOrderByCreatedAtDesc(2L))
                .willReturn(Flux.empty());

        asUser(2L).get().uri("/api/inquiries")
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("[]");
    }

    // ── GET /api/inquiries/{id} ──────────────────────────────────────────────

    @Test
    void detail_owner_returns_200() {
        given(inquiryRepository.findById(1L)).willReturn(Mono.just(INQUIRY_1));

        asUser(1L).get().uri("/api/inquiries/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.id").isEqualTo(1);
    }

    @Test
    void detail_not_owner_returns_403() {
        given(inquiryRepository.findById(1L)).willReturn(Mono.just(INQUIRY_1));

        asUser(2L).get().uri("/api/inquiries/1")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void detail_not_found_returns_404() {
        given(inquiryRepository.findById(99L)).willReturn(Mono.empty());

        asUser(1L).get().uri("/api/inquiries/99")
                .exchange()
                .expectStatus().isNotFound();
    }

    // ── DELETE /api/inquiries/{id} ───────────────────────────────────────────

    @Test
    void delete_owner_returns_204() {
        given(inquiryRepository.findById(1L)).willReturn(Mono.just(INQUIRY_1));
        given(inquiryRepository.deleteById(1L)).willReturn(Mono.empty());

        asUser(1L).delete().uri("/api/inquiries/1")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void delete_not_owner_returns_403() {
        given(inquiryRepository.findById(1L)).willReturn(Mono.just(INQUIRY_1));

        asUser(2L).delete().uri("/api/inquiries/1")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void delete_not_found_returns_404() {
        given(inquiryRepository.findById(99L)).willReturn(Mono.empty());

        asUser(1L).delete().uri("/api/inquiries/99")
                .exchange()
                .expectStatus().isNotFound();
    }

    // ── POST /api/inquiries/{id}/answer ──────────────────────────────────────

    @Test
    void answer_admin_succeeds() {
        User admin = User.builder().id(1L).role("ADMIN").build();
        Inquiry answered = INQUIRY_1.toBuilder().answer("답변입니다").status("ANSWERED").build();

        given(userRepository.findById(1L)).willReturn(Mono.just(admin));
        given(inquiryRepository.findById(1L)).willReturn(Mono.just(INQUIRY_1));
        given(inquiryRepository.save(any())).willReturn(Mono.just(answered));

        asUser(1L).post().uri("/api/inquiries/1/answer")
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
        User regular = User.builder().id(2L).tier("FREE").build();

        given(userRepository.findById(2L)).willReturn(Mono.just(regular));

        asUser(2L).post().uri("/api/inquiries/1/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("answer", "무단 답변 시도"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void answer_inquiry_not_found_returns_404() {
        User admin = User.builder().id(1L).role("ADMIN").build();

        given(userRepository.findById(1L)).willReturn(Mono.just(admin));
        given(inquiryRepository.findById(99L)).willReturn(Mono.empty());

        asUser(1L).post().uri("/api/inquiries/99/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("answer", "답변"))
                .exchange()
                .expectStatus().isNotFound();
    }
}
