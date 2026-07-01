package com.hypepia.apiverse.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

@Configuration
public class ProxyConfig {

    @Bean
    WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    RedisScript<Long> tokenBucketScript() throws IOException {
        return RedisScript.of(new ClassPathResource("lua/token_bucket.lua"), Long.class);
    }
}
