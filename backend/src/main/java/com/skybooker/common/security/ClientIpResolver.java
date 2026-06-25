package com.skybooker.common.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 从 HTTP 请求中解析客户端真实 IP。
 * nginx 代理设置 X-Real-IP / X-Forwarded-For,优先读取;无代理时 fallback 到 getRemoteAddr()。
 * 生产 nginx 会覆盖客户端伪造的 X-Real-IP(proxy_set_header X-Real-IP $remote_addr),安全。
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip.trim();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
