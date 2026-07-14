package com.hypepia.apiverse.gateway.config;

import com.hypepia.apiverse.core.kafka.BillingLogEvent;
import com.hypepia.apiverse.core.kafka.BillingLogEventSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public KafkaSender<String, BillingLogEvent> billingLogKafkaSender(
            @Value("${app.kafka.bootstrap-servers}") String bootstrapServers) {

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, BillingLogEventSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        // 브로커 응답이 오래 걸리면 ProxyService의 폴백 경로(R2DBC 직접 저장)로 넘어가야 하므로 짧게 유지
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000);

        return KafkaSender.create(SenderOptions.create(props));
    }
}
