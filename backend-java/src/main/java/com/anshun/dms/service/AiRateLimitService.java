package com.anshun.dms.service;

import com.anshun.dms.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Per-user fixed-window guard. Redis makes the primary path consistent across multiple application
 * instances; the in-memory implementation remains a safe development fallback when Redis is down.
 */
@Service
public class AiRateLimitService {
    private static final Logger log = LoggerFactory.getLogger(AiRateLimitService.class);
    private static final DefaultRedisScript<Long> INCREMENT_WITH_TTL = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
            return current
            """, Long.class);
    private final int maxRequests;
    private final boolean redisEnabled;
    private final StringRedisTemplate redis;
    private final ConcurrentHashMap<String, Deque<Instant>> requests = new ConcurrentHashMap<>();
    private final AtomicBoolean fallbackWarningLogged = new AtomicBoolean(false);

    public AiRateLimitService(@Value("${app.ai.deepseek.max-requests-per-minute}") int maxRequests,
                              @Value("${app.ai.rate-limit.redis-enabled:true}") boolean redisEnabled,
                              StringRedisTemplate redis) {
        this.maxRequests = maxRequests;
        this.redisEnabled = redisEnabled;
        this.redis = redis;
    }

    public void check(String username) {
        if (redisEnabled) {
            try {
                Long current = redis.execute(INCREMENT_WITH_TTL, java.util.List.of("dms:ai:rate:" + username), "60");
                if (current != null && current > maxRequests) {
                    throw BusinessException.tooManyRequests("AI 助手请求过于频繁，请稍后再试");
                }
                return;
            } catch (BusinessException exception) {
                throw exception;
            } catch (DataAccessException exception) {
                if (fallbackWarningLogged.compareAndSet(false, true)) {
                    log.warn("Redis AI rate limit unavailable; using single-instance fallback");
                }
            }
        }
        checkInMemory(username);
    }

    private void checkInMemory(String username) {
        Deque<Instant> window = requests.computeIfAbsent(username, ignored -> new ArrayDeque<>());
        synchronized (window) {
            Instant cutoff = Instant.now().minusSeconds(60);
            while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) window.removeFirst();
            if (window.size() >= maxRequests) throw BusinessException.tooManyRequests("AI 助手请求过于频繁，请稍后再试");
            window.addLast(Instant.now());
        }
    }
}
