package com.skybooker.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    static final String TYPE_ACCESS = "access";
    static final String TYPE_REFRESH = "refresh";
    static final String CLAIM_TYPE = "type";
    static final String CLAIM_TOKEN_VER = "tokenVer";

    private final SecretKey key;
    private final long accessMs;
    private final long refreshMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-ms}") long accessMs,
            @Value("${jwt.refresh-ms}") long refreshMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessMs = accessMs;
        this.refreshMs = refreshMs;
    }

    /**
     * 一次性签发 access + refresh token。refresh 的 jti 由调用方写入 RefreshTokenStore 以支持 logout 作废。
     */
    public TokenPair issueTokenPair(Long userId, String email, String role, String loginPortal,
                                    long tokenVer) {
        String accessToken = buildToken(userId, email, role, loginPortal,
                TYPE_ACCESS, null, accessMs, tokenVer);
        String jti = UUID.randomUUID().toString();
        String refreshToken = buildToken(userId, email, role, loginPortal,
                TYPE_REFRESH, jti, refreshMs, tokenVer);

        return new TokenPair(accessToken, refreshToken, jti,
                accessMs / 1000, refreshMs);
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验签名且 type 必须为 access，防止 refresh token 被当作 access 使用。
     */
    public boolean validateAccessToken(String token) {
        try {
            Claims claims = parseToken(token);
            return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 解析并校验 refresh token：签名有效且 type=refresh，否则抛 JwtException。
     * 调用方还需自行检查 RefreshTokenStore 中是否存在（logout 作废语义）。
     */
    public Claims parseRefreshToken(String token) {
        Claims claims = parseToken(token);
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("token is not a refresh token");
        }
        return claims;
    }

    private String buildToken(Long userId, String email, String role, String loginPortal,
                              String type, String jti, long ttlMs, long tokenVer) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("email", email)
                .claim("role", role)
                .claim("loginPortal", loginPortal)
                .claim(CLAIM_TYPE, type)
                .claim(CLAIM_TOKEN_VER, tokenVer)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs));
        if (jti != null) {
            builder.id(jti);
        }
        return builder.signWith(key).compact();
    }
}
