package com.hypepia.apiverse.eventconsumer.billinglog;

import com.hypepia.apiverse.core.kafka.BillingLogEvent;
import com.hypepia.apiverse.core.kafka.BillingLogEventDeserializer;
import com.hypepia.apiverse.core.kafka.BillingLogTopics;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class BillingLogKafkaConfig {

    @Bean
    public KafkaReceiver<String, BillingLogEvent> billingLogKafkaReceiver(
            @Value("${app.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${app.kafka.billing-log.consumer-group-id}") String groupId) {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, BillingLogEventDeserializer.class);
        // Kafka 네이티브 auto-commit 대신 reactor-kafka의 acknowledge 기반 커밋을 쓴다 —
        // DB 저장에 성공한 레코드만 acknowledge하여 at-least-once를 보장하기 위함 (BillingLogConsumer 참고).
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        ReceiverOptions<String, BillingLogEvent> receiverOptions =
                ReceiverOptions.<String, BillingLogEvent>create(props)
                        .subscription(Collections.singleton(BillingLogTopics.BILLING_LOG))
                        .commitInterval(Duration.ofSeconds(2))
                        .commitBatchSize(500);

        return KafkaReceiver.create(receiverOptions);
    }
}
