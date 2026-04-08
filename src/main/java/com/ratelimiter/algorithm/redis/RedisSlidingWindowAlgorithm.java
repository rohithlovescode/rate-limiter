package com.ratelimiter.algorithm.redis;

import com.ratelimiter.algorithm.RateLimiterAlgorithm;
import com.ratelimiter.config.RateLimiterProperties;
import com.ratelimiter.domain.Algorithm;
import com.ratelimiter.domain.RateLimitConfig;
import com.ratelimiter.domain.RateLimitResult;
import com.ratelimiter.domain.RateLimitStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class RedisSlidingWindowAlgorithm implements RateLimiterAlgorithm {

    private static final String KEY_SEGMENT = "sw";

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> slidingWindowScript;
    private final RateLimiterProperties properties;

    @Override
    public Algorithm getType() {
        return Algorithm.SLIDING_WINDOW;
    }

    @Override
    public RateLimitResult tryAcquire(String clientId, RateLimitConfig config) {
        String key       = buildKey(clientId);
        long   now       = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        List result = redisTemplate.execute(
                slidingWindowScript,
                List.of(key),
                String.valueOf(config.getWindowMs()),
                String.valueOf(config.getLimit()),
                String.valueOf(now),
                requestId
        );

        boolean allowed   = toLong(result.get(0)) == 1L;
        long    remaining = toLong(result.get(1));
        long    limit     = toLong(result.get(2));

        long retryAfterMs = 0L;
        if (!allowed) {
            retryAfterMs = computeRetryAfter(key, config.getWindowMs(), now);
        }

        return RateLimitResult.builder()
                .allowed(allowed)
                .remaining(remaining)
                .limit(limit)
                .retryAfterMs(retryAfterMs)
                .algorithm(Algorithm.SLIDING_WINDOW)
                .clientId(clientId)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public void reset(String clientId) {
        redisTemplate.delete(buildKey(clientId));
    }

    @Override
    public RateLimitStatus getStatus(String clientId, RateLimitConfig config) {
        String key         = buildKey(clientId);
        long   now         = System.currentTimeMillis();
        long   windowStart = now - config.getWindowMs();

        Long count = redisTemplate.opsForZSet().count(key, windowStart, now);
        long currentCount = count != null ? count : 0L;

        long resetAt = computeResetAt(key, config.getWindowMs(), now);

        return RateLimitStatus.builder()
                .clientId(clientId)
                .algorithm(Algorithm.SLIDING_WINDOW)
                .remaining(Math.max(0, config.getLimit() - currentCount))
                .limit(config.getLimit())
                .resetAtMs(resetAt)
                .limited(currentCount >= config.getLimit())
                .build();
    }

    private long computeRetryAfter(String key, int windowMs, long now) {
        Set<String> oldest = redisTemplate.opsForZSet().range(key, 0, 0);
        if (oldest == null || oldest.isEmpty()) return 0L;

        Double oldestScore = redisTemplate.opsForZSet().score(key, oldest.iterator().next());
        if (oldestScore == null) return 0L;

        return Math.max(0L, (long) (oldestScore + windowMs - now));
    }

    private long computeResetAt(String key, int windowMs, long now) {
        Set<String> oldest = redisTemplate.opsForZSet().range(key, 0, 0);
        if (oldest == null || oldest.isEmpty()) return now;

        Double oldestScore = redisTemplate.opsForZSet().score(key, oldest.iterator().next());
        return oldestScore != null ? (long) (oldestScore + windowMs) : now;
    }

    private String buildKey(String clientId) {
        return properties.getKeyPrefix() + ":" + KEY_SEGMENT + ":" + clientId;
    }

    private long toLong(Object o) {
        return ((Number) o).longValue();
    }
}
