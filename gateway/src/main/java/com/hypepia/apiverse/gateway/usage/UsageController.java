package com.hypepia.apiverse.gateway.usage;

import com.hypepia.apiverse.core.projection.DailyStat;
import com.hypepia.apiverse.core.repository.BillingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/usage")
@RequiredArgsConstructor
public class UsageController {

    private final BillingLogRepository billingLogRepository;

    @GetMapping("/daily")
    public Flux<DailyStat> dailyStats(@AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMapMany(billingLogRepository::findDailyStatsByUserId);
    }

    // 본인 소유 API 키의 로그만 조회 (apiProductId로 특정 API만 필터링 가능). days로 조회 기간 상한.
    @GetMapping("/logs")
    public Mono<ResponseEntity<Map<String, Object>>> logs(
            @RequestParam(required = false) Long apiProductId,
            @RequestParam(defaultValue = "false") boolean onlyErrors,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal Mono<Long> principal) {
        int safeDays = Math.min(Math.max(days, 1), 90);
        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 0);
        return principal
                .flatMap(uid -> Mono.zip(
                        billingLogRepository.findLogsPageByUserId(uid, apiProductId, onlyErrors, safeDays, safeSize, (long) safePage * safeSize).collectList(),
                        billingLogRepository.countLogsByUserId(uid, apiProductId, onlyErrors, safeDays)
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
