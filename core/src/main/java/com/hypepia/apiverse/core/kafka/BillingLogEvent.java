package com.hypepia.apiverse.core.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// gateway(producer)와 event-consumer(consumer)가 공유하는 billing_logs 적재 이벤트.
// core.entity.BillingLog와 필드가 동일하지만 R2DBC 엔티티를 Kafka 페이로드로 직접 쓰지 않기 위해 별도 DTO로 분리.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingLogEvent {

    private String apiKeyValue;
    private String requestPath;
    private String httpMethod;
    private Integer responseStatus;
    private String clientIp;
    private LocalDateTime requestTime;
}
