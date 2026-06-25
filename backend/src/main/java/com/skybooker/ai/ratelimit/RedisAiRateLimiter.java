package com.skybooker.ai.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class RedisAiRateLimiter implements AiRateLimiter {

    private static final String KEY_PREFIX = "ai:chat:ip:";
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int MAX_REQUESTS = 10;

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean tryAcquire(String ip) {
        if (ip == null || ip.isBlank()) {
            return true;
        }
        String key = KEY_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, WINDOW);
        }
        return count == null || count <= MAX_REQUESTS;
    }
}
