package com.anshun.dms.security;

import com.anshun.dms.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Protects the BCrypt login path by limiting both account and source-IP request rates. */
@Service
public class LoginRateLimitService {
    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitService.class);
    private static final DefaultRedisScript<Long> INCREMENT_WITH_TTL = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
            return current
            """, Long.class);
    private final StringRedisTemplate redis;
    private final boolean redisEnabled;
    private final int maxPerUsername;
    private final int maxPerIp;
    private final ConcurrentHashMap<String, Deque<Instant>> localWindows = new ConcurrentHashMap<>();
    private final AtomicBoolean fallbackWarningLogged = new AtomicBoolean(false);

    public LoginRateLimitService(StringRedisTemplate redis,
                                 @Value("${app.security.login-rate-limit.redis-enabled:true}") boolean redisEnabled,
                                 @Value("${app.security.login-rate-limit.max-per-username-per-minute:6}") int maxPerUsername,
                                 @Value("${app.security.login-rate-limit.max-per-ip-per-minute:20}") int maxPerIp) {
        this.redis = redis;
        this.redisEnabled = redisEnabled;
        this.maxPerUsername = maxPerUsername;
        this.maxPerIp = maxPerIp;
    }

    public void check(String username, String clientIp) {
        String userKey = "user:" + digest(username == null ? "" : username.toLowerCase());
        String ipKey = "ip:" + digest(clientIp == null ? "unknown" : clientIp);
        if (redisEnabled) {
            try {
                checkRedis(userKey, maxPerUsername);
                checkRedis(ipKey, maxPerIp);
                return;
            } catch (BusinessException exception) {
                throw exception;
            } catch (DataAccessException exception) {
                if (fallbackWarningLogged.compareAndSet(false, true)) {
                    log.warn("Redis login rate limit unavailable; using single-instance fallback");
                }
            }
        }
        checkLocal(userKey, maxPerUsername);
        checkLocal(ipKey, maxPerIp);
    }

    private void checkRedis(String key, int limit) {
        Long count = redis.execute(INCREMENT_WITH_TTL, List.of("dms:auth:rate:" + key), "60");
        if (count != null && count > limit) throw limited();
    }

    private void checkLocal(String key, int limit) {
        if (localWindows.size() > 10_000) cleanupExpired();
        Deque<Instant> window = localWindows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (window) {
            Instant cutoff = Instant.now().minusSeconds(60);
            while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) window.removeFirst();
            if (window.size() >= limit) throw limited();
            window.addLast(Instant.now());
        }
    }

    private void cleanupExpired() {
        Instant cutoff = Instant.now().minusSeconds(60);
        localWindows.entrySet().removeIf(entry -> {
            Deque<Instant> window = entry.getValue();
            synchronized (window) {
                while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) window.removeFirst();
                return window.isEmpty();
            }
        });
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private BusinessException limited() {
        return BusinessException.tooManyRequests("登录请求过于频繁，请一分钟后重试");
    }
}
