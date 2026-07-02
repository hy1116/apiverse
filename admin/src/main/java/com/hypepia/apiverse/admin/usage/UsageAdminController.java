package com.hypepia.apiverse.admin.usage;

import com.hypepia.apiverse.core.projection.DailyStat;
import com.hypepia.apiverse.core.repository.BillingLogRepository;
import com.hypepia.apiverse.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

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

    // 전체 billing_logs 조회 (onlyErrors=true면 5xx 건만), 최신순 페이지네이션. days로 조회 기간 상한.
    @GetMapping("/logs")
    public Mono<ResponseEntity<Map<String, Object>>> logs(
            @RequestParam(defaultValue = "false") boolean onlyErrors,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal Mono<Long> principal) {
        int safeDays = Math.min(Math.max(days, 1), 90);
        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 0);
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMap(uid -> Mono.zip(
                        billingLogRepository.findLogsPage(onlyErrors, safeDays, safeSize, (long) safePage * safeSize).collectList(),
                        billingLogRepository.countLogs(onlyErrors, safeDays)
                ))
                .map(tuple -> {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("items", tuple.getT1());
                    body.put("total", tuple.getT2());
                    body.put("page", safePage);
                    body.put("size", safeSize);
                    return ResponseEntity.ok(body);
                });
    }
}
