package com.hypepia.apiverse.gateway.usage;

import com.hypepia.apiverse.core.dto.DailyStat;
import com.hypepia.apiverse.core.repository.BillingLogRepository;
import com.hypepia.apiverse.gateway.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import static org.mockito.BDDMockito.given;

@WebFluxTest(controllers = UsageController.class)
@Import(TestSecurityConfig.class)
class UsageControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private BillingLogRepository billingLogRepository;

    @Test
    void dailyStats_returns_stats_list() {
        given(billingLogRepository.findDailyStats()).willReturn(Flux.just(
                new DailyStat("06/29", 110L, 3L),
                new DailyStat("06/30", 88L, 2L),
                new DailyStat("07/01", 45L, 0L)
        ));

        webTestClient.get().uri("/api/usage/daily")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[0].date").isEqualTo("06/29")
                .jsonPath("$[0].requests").isEqualTo(110)
                .jsonPath("$[0].errors").isEqualTo(3)
                .jsonPath("$[2].date").isEqualTo("07/01");
    }

    @Test
    void dailyStats_empty_returns_empty_array() {
        given(billingLogRepository.findDailyStats()).willReturn(Flux.empty());

        webTestClient.get().uri("/api/usage/daily")
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("[]");
    }

    @Test
    void dailyStats_does_not_require_authentication() {
        given(billingLogRepository.findDailyStats()).willReturn(Flux.empty());

        // Authorization 헤더 없이 호출해도 200 반환
        webTestClient.get().uri("/api/usage/daily")
                .exchange()
                .expectStatus().isOk();
    }
}
