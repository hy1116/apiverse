package com.hypepia.apiverse.core.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.nio.charset.StandardCharsets;

public class BillingLogEventDeserializer implements Deserializer<BillingLogEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public BillingLogEvent deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.readValue(new String(data, StandardCharsets.UTF_8), BillingLogEvent.class);
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize BillingLogEvent", e);
        }
    }
}
