package com.skybooker.ai.ratelimit;

/**
 * IP 维度限流(AI 接口匿名防刷)。
 * 单次 tryAcquire 保证原子性:INCR 后立即判断,并发请求不会同时通过。
 */
public interface AiRateLimiter {

    /**
     * 尝试获取一个请求配额。
     * @return true=允许(已计入),false=已被限流
     */
    boolean tryAcquire(String ip);
}
