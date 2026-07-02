package com.hypepia.apiverse.admin.usage;

import com.hypepia.apiverse.admin.config.TestSecurityConfig;
import com.hypepia.apiverse.core.entity.BillingLog;
import com.hypepia.apiverse.core.entity.User;
import com.hypepia.apiverse.core.projection.DailyStat;
import com.hypepia.apiverse.core.repository.BillingLogRepository;
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

@WebFluxTest(controllers = UsageAdminController.class)
@Import(TestSecurityConfig.class)
class UsageAdminControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private BillingLogRepository billingLogRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final User ADMIN = User.builder().id(1L).role("ADMIN").build();
    private static final User REGULAR = User.builder().id(2L).tier("FREE").build();

    private WebTestClient asUser(long userId) {
        return webTestClient.mutateWith(mockAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of())));
    }

    @Test
    void dailyStats_admin_returns_global_stats() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(billingLogRepository.findDailyStatsGlobal()).willReturn(Flux.just(
                new DailyStat("07/01", 500L, 12L)
        ));

        asUser(1L).get().uri("/api/admin/usage/daily")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].requests").isEqualTo(500);
    }

    @Test
    void dailyStats_non_admin_returns_403() {
        given(userRepository.findById(2L)).willReturn(Mono.just(REGULAR));

        asUser(2L).get().uri("/api/admin/usage/daily")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void logs_admin_returns_paginated_items() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(billingLogRepository.findLogsPage(false, 7, 50, 0L)).willReturn(Flux.just(
                BillingLog.builder().id(1L).apiKeyValue("key1").requestPath("/current").httpMethod("GET")
                        .responseStatus(200).clientIp("127.0.0.1").build()
        ));
        given(billingLogRepository.countLogs(false, 7)).willReturn(Mono.just(1L));

        asUser(1L).get().uri("/api/admin/usage/logs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.total").isEqualTo(1)
                .jsonPath("$.items.length()").isEqualTo(1)
                .jsonPath("$.items[0].responseStatus").isEqualTo(200);
    }

    @Test
    void logs_onlyErrors_filters_to_5xx() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(billingLogRepository.findLogsPage(true, 7, 50, 0L)).willReturn(Flux.just(
                BillingLog.builder().id(2L).apiKeyValue("key1").requestPath("/current").httpMethod("GET")
                        .responseStatus(500).clientIp("127.0.0.1").build()
        ));
        given(billingLogRepository.countLogs(true, 7)).willReturn(Mono.just(1L));

        asUser(1L).get().uri("/api/admin/usage/logs?onlyErrors=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items[0].responseStatus").isEqualTo(500);
    }

    @Test
    void logs_non_admin_returns_403() {
        given(userRepository.findById(2L)).willReturn(Mono.just(REGULAR));

        asUser(2L).get().uri("/api/admin/usage/logs")
                .exchange()
                .expectStatus().isForbidden();
    }
}
