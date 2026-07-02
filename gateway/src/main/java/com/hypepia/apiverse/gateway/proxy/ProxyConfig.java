package com.hypepia.apiverse.gateway.proxy;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.time.Duration;

@Configuration
public class ProxyConfig {

    // 업스트림이 연결만 되고 응답을 주지 않으면 타임아웃 없이는 무한 대기하게 되므로 connect/response 타임아웃을 명시한다.
    @Bean
    WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
                .responseTimeout(Duration.ofSeconds(10));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    @Bean
    RedisScript<Long> tokenBucketScript() {
        return RedisScript.of(new ClassPathResource("lua/token_bucket.lua"), Long.class);
    }
}
