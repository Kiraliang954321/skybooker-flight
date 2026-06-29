package com.skybooker.ai.ratelimit;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("test")
public class InMemoryAiRateLimiter implements AiRateLimiter {

    private static final long WINDOW_MS = 60_000L;
    private static final int MAX_REQUESTS = 10;

    private final Map<String, long[]> counters = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String ip) {
        if (ip == null || ip.isBlank()) {
            return true;
        }
        long now = System.currentTimeMillis();
        long[] result = counters.compute(ip, (k, existing) -> {
            if (existing == null || now > existing[1]) {
                return new long[]{1, now + WINDOW_MS};
            }
            existing[0]++;
            return existing;
        });
        return result[0] <= MAX_REQUESTS;
    }
}
