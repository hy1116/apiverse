package com.hypepia.apiverse.admin.stats;

import com.hypepia.apiverse.core.projection.ApiKeyErrorStat;
import com.hypepia.apiverse.core.projection.ProductErrorStat;
import com.hypepia.apiverse.core.projection.QuotaUsageStat;
import com.hypepia.apiverse.core.projection.StatusCodeStat;
import com.hypepia.apiverse.core.repository.ApiKeyRepository;
import com.hypepia.apiverse.core.repository.BillingLogRepository;
import com.hypepia.apiverse.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class StatsAdminController {

    private final BillingLogRepository billingLogRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;

    private Mono<Void> requireAdmin(Long uid) {
        return userRepository.findById(uid)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED)))
                .flatMap(user -> "ADMIN".equals(user.getRole())
                        ? Mono.empty()
                        : Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN)));
    }

    private static int safeDays(int days) {
        return Math.min(Math.max(days, 1), 90);
    }

    // 상품별 에러 랭킹
    @GetMapping("/products-errors")
    public Flux<ProductErrorStat> productErrors(@RequestParam(defaultValue = "7") int days,
                                                @AuthenticationPrincipal Mono<Long> principal) {
        int safeDays = safeDays(days);
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMapMany(uid -> billingLogRepository.findProductErrorStats(safeDays));
    }

    // 에러를 가장 많이 겪는 API 키 Top N
    @GetMapping("/top-error-keys")
    public Flux<ApiKeyErrorStat> topErrorKeys(@RequestParam(defaultValue = "7") int days,
                                              @RequestParam(defaultValue = "20") int limit,
                                              @AuthenticationPrincipal Mono<Long> principal) {
        int safeDays = safeDays(days);
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMapMany(uid -> billingLogRepository.findTopErrorApiKeys(safeDays, safeLimit));
    }

    // 상태코드 분포
    @GetMapping("/status-codes")
    public Flux<StatusCodeStat> statusCodes(@RequestParam(defaultValue = "7") int days,
                                            @AuthenticationPrincipal Mono<Long> principal) {
        int safeDays = safeDays(days);
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMapMany(uid -> billingLogRepository.findStatusCodeStats(safeDays));
    }

    // 상품별 사용량 랭킹 (호출량 기준)
    @GetMapping("/products-usage")
    public Flux<ProductErrorStat> productsUsage(@RequestParam(defaultValue = "7") int days,
                                                @AuthenticationPrincipal Mono<Long> principal) {
        int safeDays = safeDays(days);
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMapMany(uid -> billingLogRepository.findMostUsedProducts(safeDays));
    }

    // 호출량이 가장 많은 API 키 Top N
    @GetMapping("/top-usage-keys")
    public Flux<ApiKeyErrorStat> topUsageKeys(@RequestParam(defaultValue = "7") int days,
                                              @RequestParam(defaultValue = "20") int limit,
                                              @AuthenticationPrincipal Mono<Long> principal) {
        int safeDays = safeDays(days);
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMapMany(uid -> billingLogRepository.findMostActiveApiKeys(safeDays, safeLimit));
    }

    // 월 쿼터 사용률 Top N (monthlyQuota=-1 무제한 제외)
    @GetMapping("/quota-usage")
    public Flux<QuotaUsageStat> quotaUsage(@RequestParam(defaultValue = "20") int limit,
                                           @AuthenticationPrincipal Mono<Long> principal) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMapMany(uid -> apiKeyRepository.findQuotaUsageStats(safeLimit));
    }
}
