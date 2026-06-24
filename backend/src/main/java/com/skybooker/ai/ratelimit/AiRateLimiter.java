package com.skybooker.ai.ratelimit;

/**
 * IP 维度限流(AI 接口匿名防刷)。
 * 实现:Redis(生产)/ InMemory(test),同 LoginRateLimiter 模式但只 IP 维度。
 */
public interface AiRateLimiter {

    boolean isLimited(String ip);

    void recordRequest(String ip);
}
