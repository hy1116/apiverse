package com.hypepia.apiverse.gateway.usage;

import com.hypepia.apiverse.core.entity.BillingLog;
import com.hypepia.apiverse.core.projection.DailyStat;
import com.hypepia.apiverse.core.repository.BillingLogRepository;
import com.hypepia.apiverse.gateway.config.TestSecurityConfig;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

@WebFluxTest(controllers = UsageController.class)
@Import(TestSecurityConfig.class)
class UsageControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private BillingLogRepository billingLogRepository;

    private WebTestClient asUser(long userId) {
        return webTestClient.mutateWith(mockAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of())));
    }

    @Test
    void dailyStats_returns_only_current_user_stats() {
        given(billingLogRepository.findDailyStatsByUserId(1L)).willReturn(Flux.just(
                new DailyStat("06/29", 110L, 3L),
                new DailyStat("06/30", 88L, 2L),
                new DailyStat("07/01", 45L, 0L)
        ));

        asUser(1L).get().uri("/api/usage/daily")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[0].date").isEqualTo("06/29")
                .jsonPath("$[0].requests").isEqualTo(110)
                .jsonPath("$[0].errors").isEqualTo(3);
    }

    @Test
    void dailyStats_different_users_see_different_data() {
        given(billingLogRepository.findDailyStatsByUserId(1L)).willReturn(Flux.just(
                new DailyStat("07/01", 100L, 0L)
        ));
        given(billingLogRepository.findDailyStatsByUserId(2L)).willReturn(Flux.just(
                new DailyStat("07/01", 5L, 1L)
        ));

        asUser(1L).get().uri("/api/usage/daily")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$[0].requests").isEqualTo(100);

        asUser(2L).get().uri("/api/usage/daily")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$[0].requests").isEqualTo(5);
    }

    @Test
    void dailyStats_no_keys_returns_empty_array() {
        given(billingLogRepository.findDailyStatsByUserId(99L)).willReturn(Flux.empty());

        asUser(99L).get().uri("/api/usage/daily")
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("[]");
    }

    // ── GET /api/usage/logs ───────────────────────────────────────────────────

    @Test
    void logs_returns_only_current_user_logs() {
        BillingLog log = BillingLog.builder()
                .id(1L).apiKeyValue("apiverse_sandbox_abc").requestPath("/current")
                .httpMethod("GET").responseStatus(200).clientIp("127.0.0.1").build();

        given(billingLogRepository.findLogsPageByUserId(eq(1L), isNull(), eq(false), eq(7), eq(50), eq(0L)))
                .willReturn(Flux.just(log));
        given(billingLogRepository.countLogsByUserId(eq(1L), isNull(), eq(false), eq(7)))
                .willReturn(Mono.just(1L));

        asUser(1L).get().uri("/api/usage/logs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.total").isEqualTo(1)
                .jsonPath("$.items[0].apiKeyValue").isEqualTo("apiverse_sandbox_abc");
    }

    @Test
    void logs_filters_by_apiProductId() {
        given(billingLogRepository.findLogsPageByUserId(eq(1L), eq(5L), eq(false), eq(7), eq(50), eq(0L)))
                .willReturn(Flux.empty());
        given(billingLogRepository.countLogsByUserId(eq(1L), eq(5L), eq(false), eq(7)))
                .willReturn(Mono.just(0L));

        asUser(1L).get().uri("/api/usage/logs?apiProductId=5")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.total").isEqualTo(0);
    }

    @Test
    void logs_onlyErrors_filters_to_5xx() {
        BillingLog log = BillingLog.builder()
                .id(2L).apiKeyValue("apiverse_sandbox_abc").requestPath("/current")
                .httpMethod("GET").responseStatus(500).clientIp("127.0.0.1").build();

        given(billingLogRepository.findLogsPageByUserId(eq(1L), isNull(), eq(true), eq(7), eq(50), eq(0L)))
                .willReturn(Flux.just(log));
        given(billingLogRepository.countLogsByUserId(eq(1L), isNull(), eq(true), eq(7)))
                .willReturn(Mono.just(1L));

        asUser(1L).get().uri("/api/usage/logs?onlyErrors=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items[0].responseStatus").isEqualTo(500);
    }
}
