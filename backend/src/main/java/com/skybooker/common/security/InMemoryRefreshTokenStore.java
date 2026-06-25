package com.skybooker.common.security;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 测试环境的内存实现。测试 profile 关闭了 Redis 自动装配，
 * 此类保证 RefreshTokenStore 在无 Redis 时仍可注入。
 */
@Component
@Profile("test")
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private record Entry(long userId, long expireAt) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final Map<String, Long> versions = new ConcurrentHashMap<>();

    @Override
    public void store(String portal, String jti, Long userId, Duration ttl) {
        store.put(key(portal, jti),
                new Entry(userId, System.currentTimeMillis() + ttl.toMillis()));
    }

    @Override
    public Long findUserId(String portal, String jti) {
        Entry entry = store.get(key(portal, jti));
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expireAt()) {
            store.remove(key(portal, jti));
            return null;
        }
        return entry.userId();
    }

    @Override
    public void revoke(String portal, String jti) {
        store.remove(key(portal, jti));
    }

    @Override
    public long currentVersion(String portal, Long userId) {
        return versions.getOrDefault(versionKey(portal, userId), 0L);
    }

    @Override
    public void revokeAllByUser(String portal, Long userId) {
        versions.merge(versionKey(portal, userId), 1L, Long::sum);
    }

    private String key(String portal, String jti) {
        return portal + ":" + jti;
    }

    private String versionKey(String portal, Long userId) {
        return portal + ":" + userId;
    }
}
