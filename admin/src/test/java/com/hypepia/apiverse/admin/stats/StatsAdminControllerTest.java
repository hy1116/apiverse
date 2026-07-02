package com.hypepia.apiverse.admin.stats;

import com.hypepia.apiverse.admin.config.TestSecurityConfig;
import com.hypepia.apiverse.core.entity.User;
import com.hypepia.apiverse.core.projection.ApiKeyErrorStat;
import com.hypepia.apiverse.core.projection.ProductErrorStat;
import com.hypepia.apiverse.core.projection.QuotaUsageStat;
import com.hypepia.apiverse.core.projection.StatusCodeStat;
import com.hypepia.apiverse.core.repository.ApiKeyRepository;
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

@WebFluxTest(controllers = StatsAdminController.class)
@Import(TestSecurityConfig.class)
class StatsAdminControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private BillingLogRepository billingLogRepository;

    @MockitoBean
    private ApiKeyRepository apiKeyRepository;

    @MockitoBean
    private UserRepository userRepository;

    private static final User ADMIN = User.builder().id(1L).role("ADMIN").build();
    private static final User REGULAR = User.builder().id(2L).tier("FREE").build();

    private WebTestClient asUser(long userId) {
        return webTestClient.mutateWith(mockAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of())));
    }

    @Test
    void productErrors_admin_returns_ranking() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(billingLogRepository.findProductErrorStats(7)).willReturn(Flux.just(
                new ProductErrorStat("기상청 날씨 API", "weather", 500L, 12L)
        ));

        asUser(1L).get().uri("/api/admin/stats/products-errors")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].productCode").isEqualTo("weather")
                .jsonPath("$[0].errorCount").isEqualTo(12);
    }

    @Test
    void productErrors_non_admin_returns_403() {
        given(userRepository.findById(2L)).willReturn(Mono.just(REGULAR));

        asUser(2L).get().uri("/api/admin/stats/products-errors")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void topErrorKeys_admin_returns_ranking() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(billingLogRepository.findTopErrorApiKeys(7, 20)).willReturn(Flux.just(
                new ApiKeyErrorStat("apiverse_sandbox_abc", "dev@hypepia.com", "공공 주소 검색 API", 100L, 8L)
        ));

        asUser(1L).get().uri("/api/admin/stats/top-error-keys")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].userEmail").isEqualTo("dev@hypepia.com")
                .jsonPath("$[0].errorCount").isEqualTo(8);
    }

    @Test
    void statusCodes_admin_returns_distribution() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(billingLogRepository.findStatusCodeStats(7)).willReturn(Flux.just(
                new StatusCodeStat(200, 900L),
                new StatusCodeStat(500, 15L)
        ));

        asUser(1L).get().uri("/api/admin/stats/status-codes")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[1].responseStatus").isEqualTo(500);
    }

    @Test
    void statusCodes_non_admin_returns_403() {
        given(userRepository.findById(2L)).willReturn(Mono.just(REGULAR));

        asUser(2L).get().uri("/api/admin/stats/status-codes")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void productsUsage_admin_returns_ranking() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(billingLogRepository.findMostUsedProducts(7)).willReturn(Flux.just(
                new ProductErrorStat("기상청 날씨 API", "weather", 500L, 12L)
        ));

        asUser(1L).get().uri("/api/admin/stats/products-usage")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].totalRequests").isEqualTo(500);
    }

    @Test
    void topUsageKeys_admin_returns_ranking() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(billingLogRepository.findMostActiveApiKeys(7, 20)).willReturn(Flux.just(
                new ApiKeyErrorStat("apiverse_sandbox_abc", "dev@hypepia.com", "공공 주소 검색 API", 100L, 8L)
        ));

        asUser(1L).get().uri("/api/admin/stats/top-usage-keys")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].totalRequests").isEqualTo(100);
    }

    @Test
    void quotaUsage_admin_returns_ranking() {
        given(userRepository.findById(1L)).willReturn(Mono.just(ADMIN));
        given(apiKeyRepository.findQuotaUsageStats(20)).willReturn(Flux.just(
                new QuotaUsageStat("apiverse_sandbox_abc", "dev@hypepia.com", "기상청 날씨 API", 1000, 950)
        ));

        asUser(1L).get().uri("/api/admin/stats/quota-usage")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].usedQuota").isEqualTo(950)
                .jsonPath("$[0].monthlyQuota").isEqualTo(1000);
    }

    @Test
    void quotaUsage_non_admin_returns_403() {
        given(userRepository.findById(2L)).willReturn(Mono.just(REGULAR));

        asUser(2L).get().uri("/api/admin/stats/quota-usage")
                .exchange()
                .expectStatus().isForbidden();
    }
}
