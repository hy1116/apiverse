package com.hypepia.apiverse.gateway.proxy;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisScript<Long> tokenBucketScript;

    public Mono<Boolean> isAllowed(String apiKeyValue, Integer callsPerSec) {
        if (callsPerSec == null || callsPerSec <= 0) {
            return Mono.just(true);
        }
        String key = "rl:" + apiKeyValue;
        long now = System.currentTimeMillis();

        return redisTemplate.execute(
                tokenBucketScript,
                List.of(key),
                String.valueOf(callsPerSec),
                String.valueOf(now)
        ).next().map(result -> result == 1L).defaultIfEmpty(false);
    }
}
