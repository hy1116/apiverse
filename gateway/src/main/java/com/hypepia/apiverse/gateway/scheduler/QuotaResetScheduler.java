package com.hypepia.apiverse.gateway.scheduler;

import com.hypepia.apiverse.core.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuotaResetScheduler {

    private final ApiKeyRepository apiKeyRepository;

    // 매월 1일 자정에 used_quota 전체 초기화
    @Scheduled(cron = "0 0 0 1 * *")
    public void resetMonthlyQuotas() {
        log.info("Monthly quota reset started");
        apiKeyRepository.resetAllUsedQuota()
                .doOnSuccess(v -> log.info("Monthly quota reset completed"))
                .doOnError(e -> log.error("Monthly quota reset failed: {}", e.getMessage()))
                .subscribe();
    }
}
