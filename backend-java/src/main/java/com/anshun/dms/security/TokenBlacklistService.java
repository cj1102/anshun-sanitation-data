package com.anshun.dms.security;

import io.jsonwebtoken.Claims;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.anshun.dms.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "app.security.token-blacklist-enabled", havingValue = "true")
public class TokenBlacklistService {
    private static final String PREFIX = "auth:blacklist:";
    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private final StringRedisTemplate redis;
    private final JwtTokenService jwt;
    private final boolean failClosed;
    private final Map<String, Instant> localFallback = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicBoolean redisWarningLogged = new java.util.concurrent.atomic.AtomicBoolean(false);

    public TokenBlacklistService(StringRedisTemplate redis, JwtTokenService jwt,
                                 @Value("${app.security.token-blacklist-fail-closed:false}") boolean failClosed) {
        this.redis = redis;
        this.jwt = jwt;
        this.failClosed = failClosed;
    }

    public void revoke(String token) {
        Claims claims = jwt.parse(token);
        Duration ttl = Duration.between(Instant.now(), claims.getExpiration().toInstant());
        String tokenId = claims.getId();
        if (tokenId == null || ttl.isNegative() || ttl.isZero()) return;
        cleanupLocalFallback();
        localFallback.put(tokenId, claims.getExpiration().toInstant());
        try {
            redis.opsForValue().set(PREFIX + tokenId, "1", ttl);
        } catch (RuntimeException exception) {
            warnRedisUnavailable();
            if (failClosed) throw BusinessException.unavailable("认证撤销服务暂时不可用，请稍后重试");
        }
    }

    public boolean isRevoked(String token) {
        Claims claims = jwt.parse(token);
        String tokenId = claims.getId();
        if (tokenId == null) return true;
        Instant expiresAt = localFallback.get(tokenId);
        if (expiresAt != null) {
            if (expiresAt.isAfter(Instant.now())) return true;
            localFallback.remove(tokenId);
        }
        try {
            return Boolean.TRUE.equals(redis.hasKey(PREFIX + tokenId));
        } catch (RuntimeException exception) {
            warnRedisUnavailable();
            if (failClosed) throw BusinessException.unavailable("认证服务暂时不可用，请稍后重试");
            return false;
        }
    }

    private void cleanupLocalFallback() {
        if (localFallback.size() < 10_000) return;
        Instant now = Instant.now();
        localFallback.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    }

    private void warnRedisUnavailable() {
        if (redisWarningLogged.compareAndSet(false, true)) {
            log.warn("Redis JWT blacklist unavailable; using configured failure policy");
        }
    }
}
