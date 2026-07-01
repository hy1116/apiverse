package com.hypepia.apiverse.gateway.usage;

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

import java.util.List;

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
}
