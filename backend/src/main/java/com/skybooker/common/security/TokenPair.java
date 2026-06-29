package com.skybooker.common.security;

/**
 * 一次签发的 access + refresh token 组合。
 *
 * @param accessToken       短期 access token，业务接口用它鉴权
 * @param refreshToken      长期 refresh token，仅用于 /refresh 与 /logout
 * @param jti               refresh token 的唯一 id，用作 RefreshTokenStore 的 key
 * @param accessExpiresInSec access token 剩余秒数，回填给前端 expiresIn
 * @param refreshTtlMs      refresh token 的全量寿命（毫秒），写入 RefreshTokenStore 的 TTL
 */
public record TokenPair(
        String accessToken,
        String refreshToken,
        String jti,
        long accessExpiresInSec,
        long refreshTtlMs
) {
}
