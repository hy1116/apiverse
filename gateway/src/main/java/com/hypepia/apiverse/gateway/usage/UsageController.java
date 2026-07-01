package com.hypepia.apiverse.gateway.usage;

import com.hypepia.apiverse.core.projection.DailyStat;
import com.hypepia.apiverse.core.repository.BillingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/usage")
@RequiredArgsConstructor
public class UsageController {

    private final BillingLogRepository billingLogRepository;

    @GetMapping("/daily")
    public Flux<DailyStat> dailyStats(@AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMapMany(billingLogRepository::findDailyStatsByUserId);
    }
}
