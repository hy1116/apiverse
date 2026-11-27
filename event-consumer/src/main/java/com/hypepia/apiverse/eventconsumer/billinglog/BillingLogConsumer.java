package com.hypepia.apiverse.eventconsumer.billinglog;

import com.hypepia.apiverse.core.entity.BillingLog;
import com.hypepia.apiverse.core.kafka.BillingLogEvent;
import com.hypepia.apiverse.core.repository.BillingLogRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;

import java.time.Duration;
import java.util.List;

// billing-log 토픽을 배치로 모아 billing_logs에 적재한다. DB 저장에 성공한 레코드만 acknowledge하므로
// 일부 레코드 저장이 실패해도 해당 파티션 오프셋 커밋이 멈춰 재기동 시 그 지점부터 재전달된다(at-least-once,
// 중복 삽입 가능성은 분석용 로그 테이블 특성상 1단계에서는 허용).
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingLogConsumer {

    private final KafkaReceiver<String, BillingLogEvent> billingLogKafkaReceiver;
    private final BillingLogRepository billingLogRepository;

    @Value("${app.kafka.billing-log.batch-max-size:500}")
    private int batchMaxSize;

    @Value("${app.kafka.billing-log.batch-max-time:2s}")
    private Duration batchMaxTime;

    @PostConstruct
    public void start() {
        billingLogKafkaReceiver.receive()
                .bufferTimeout(batchMaxSize, batchMaxTime)
                .concatMap(this::processBatch)
                .doOnError(e -> log.error("billing-log consumer stream terminated", e))
                .subscribe();
    }

    private Mono<Void> processBatch(List<ReceiverRecord<String, BillingLogEvent>> batch) {
        return Flux.fromIterable(batch)
                .flatMap(record -> billingLogRepository.save(toEntity(record.value()))
                        .doOnSuccess(saved -> record.receiverOffset().acknowledge())
                        .onErrorResume(e -> {
                            log.error("Failed to persist billing log (key={}): {}", record.key(), e.getMessage());
                            return Mono.empty();
                        }))
                .then();
    }

    private BillingLog toEntity(BillingLogEvent event) {
        return BillingLog.builder()
                .apiKeyValue(event.getApiKeyValue())
                .requestPath(event.getRequestPath())
                .httpMethod(event.getHttpMethod())
                .responseStatus(event.getResponseStatus())
                .clientIp(event.getClientIp())
                .requestTime(event.getRequestTime())
                .build();
    }
}
