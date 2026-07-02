package com.hypepia.apiverse.admin.usage;

import com.hypepia.apiverse.core.projection.DailyStat;
import com.hypepia.apiverse.core.repository.BillingLogRepository;
import com.hypepia.apiverse.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/usage")
@RequiredArgsConstructor
public class UsageAdminController {

    private final BillingLogRepository billingLogRepository;
    private final UserRepository userRepository;

    private Mono<Void> requireAdmin(Long uid) {
        return userRepository.findById(uid)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED)))
                .flatMap(user -> "ADMIN".equals(user.getRole())
                        ? Mono.empty()
                        : Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN)));
    }

    @GetMapping("/daily")
    public Flux<DailyStat> dailyStats(@AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMapMany(uid -> billingLogRepository.findDailyStatsGlobal());
    }
}
