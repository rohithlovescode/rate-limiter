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

@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"unchecked", "rawtypes"})
public class RedisTokenBucketAlgorithm implements RateLimiterAlgorithm {

    private static final String KEY_SEGMENT = "tb";

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> tokenBucketScript;
    private final RateLimiterProperties properties;

    @Override
    public Algorithm getType() {
        return Algorithm.TOKEN_BUCKET;
    }

    @Override
    public RateLimitResult tryAcquire(String clientId, RateLimitConfig config) {
        String key   = buildKey(clientId);
        long   now   = System.currentTimeMillis();

        List result = redisTemplate.execute(
                tokenBucketScript,
                List.of(key),
                String.valueOf(config.getCapacity()),
                String.valueOf(config.getRefillRate()),
                String.valueOf(now),
                "1"
        );

        boolean allowed   = toLong(result.get(0)) == 1L;
        long    remaining = toLong(result.get(1));
        long    capacity  = toLong(result.get(2));

        // Time (ms) until next token is available
        long retryAfterMs = allowed ? 0L : (long) Math.ceil(1000.0 / config.getRefillRate());

        return RateLimitResult.builder()
                .allowed(allowed)
                .remaining(remaining)
                .limit(capacity)
                .retryAfterMs(retryAfterMs)
                .algorithm(Algorithm.TOKEN_BUCKET)
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
        String key = buildKey(clientId);

        List<Object> data = redisTemplate.opsForHash()
                .multiGet(key, List.of("tokens", "last_refill"));

        double tokens     = data.get(0) != null ? Double.parseDouble((String) data.get(0)) : config.getCapacity();
        long   lastRefill = data.get(1) != null ? Long.parseLong((String) data.get(1))    : System.currentTimeMillis();

        // Estimate when bucket will be full
        double deficit  = config.getCapacity() - tokens;
        long   resetAt  = lastRefill + (long) Math.ceil(deficit / config.getRefillRate() * 1000);

        return RateLimitStatus.builder()
                .clientId(clientId)
                .algorithm(Algorithm.TOKEN_BUCKET)
                .remaining((long) tokens)
                .limit(config.getCapacity())
                .resetAtMs(resetAt)
                .limited(tokens < 1)
                .build();
    }

    private String buildKey(String clientId) {
        return properties.getKeyPrefix() + ":" + KEY_SEGMENT + ":" + clientId;
    }

    private long toLong(Object o) {
        return ((Number) o).longValue();
    }
}
