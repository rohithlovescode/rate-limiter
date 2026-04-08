package com.ratelimiter.service.impl;

import com.ratelimiter.algorithm.local.LocalSlidingWindowAlgorithm;
import com.ratelimiter.algorithm.local.LocalTokenBucketAlgorithm;
import com.ratelimiter.algorithm.redis.RedisSlidingWindowAlgorithm;
import com.ratelimiter.algorithm.redis.RedisTokenBucketAlgorithm;
import com.ratelimiter.config.RateLimiterProperties;
import com.ratelimiter.domain.*;
import com.ratelimiter.metrics.RateLimiterMetrics;
import com.ratelimiter.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RedisRateLimiterService implements RateLimiterService {

    private final RedisTokenBucketAlgorithm   redisTokenBucket;
    private final RedisSlidingWindowAlgorithm redisSlidingWindow;
    private final LocalTokenBucketAlgorithm   localTokenBucket;
    private final LocalSlidingWindowAlgorithm localSlidingWindow;
    private final RateLimiterProperties       properties;
    private final RateLimiterMetrics          metrics;

    @Override
    public RateLimitResult check(String clientId, RateLimitConfig config) {
        RateLimitConfig resolved  = resolve(clientId, config);
        Algorithm       algo      = resolved.getAlgorithm();
        FallbackStrategy fallback = resolved.getFallbackStrategy();

        try {
            RateLimitResult result = executeRedis(clientId, resolved, algo);
            metrics.record(result, false);
            return result;
        } catch (Exception e) {
            log.warn("Redis unavailable for client='{}'. Applying fallback={}", clientId, fallback);
            metrics.recordRedisError();
            return handleFallback(clientId, resolved, algo, fallback);
        }
    }

    @Override
    public RateLimitResult check(String clientId) {
        return check(clientId, RateLimitConfig.builder().build());
    }

    @Override
    public RateLimitStatus getStatus(String clientId, RateLimitConfig config) {
        RateLimitConfig resolved = resolve(clientId, config);
        try {
            return switch (resolved.getAlgorithm()) {
                case TOKEN_BUCKET   -> redisTokenBucket.getStatus(clientId, resolved);
                case SLIDING_WINDOW -> redisSlidingWindow.getStatus(clientId, resolved);
            };
        } catch (Exception e) {
            log.warn("Redis unavailable for getStatus, returning empty status for client='{}'", clientId);
            return RateLimitStatus.builder()
                    .clientId(clientId).algorithm(resolved.getAlgorithm())
                    .remaining(resolved.getLimit()).limit(resolved.getLimit())
                    .resetAtMs(System.currentTimeMillis()).limited(false).build();
        }
    }

    @Override
    public void reset(String clientId) {
        try {
            redisTokenBucket.reset(clientId);
            redisSlidingWindow.reset(clientId);
        } catch (Exception e) {
            log.warn("Redis unavailable during reset for client='{}'", clientId);
        }
        // Always reset local fallback state too
        localTokenBucket.reset(clientId);
        localSlidingWindow.reset(clientId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private RateLimitResult executeRedis(String clientId, RateLimitConfig config, Algorithm algo) {
        return switch (algo) {
            case TOKEN_BUCKET   -> redisTokenBucket.tryAcquire(clientId, config);
            case SLIDING_WINDOW -> redisSlidingWindow.tryAcquire(clientId, config);
        };
    }

    private RateLimitResult handleFallback(String clientId, RateLimitConfig config,
                                            Algorithm algo, FallbackStrategy strategy) {
        return switch (strategy) {
            case AP -> {
                // Available: use local in-memory rate limiter
                RateLimitResult result = switch (algo) {
                    case TOKEN_BUCKET   -> localTokenBucket.tryAcquire(clientId, config);
                    case SLIDING_WINDOW -> localSlidingWindow.tryAcquire(clientId, config);
                };
                metrics.record(result, true);
                yield result;
            }
            case CP ->
                // Consistent: fail closed — deny all requests when Redis is down
                RateLimitResult.denied(clientId, config.getLimit(), algo, -1L,
                        "Rate limiter unavailable (CP fail-closed mode)");
        };
    }

    /**
     * Merge request config with application defaults.
     * Any null/zero field in `config` is replaced with default from properties.
     */
    private RateLimitConfig resolve(String clientId, RateLimitConfig config) {
        if (config == null) config = RateLimitConfig.builder().build();
        return RateLimitConfig.builder()
                .clientId(clientId)
                .algorithm(config.getAlgorithm() != null
                        ? config.getAlgorithm() : properties.getDefaultAlgorithm())
                .fallbackStrategy(config.getFallbackStrategy() != null
                        ? config.getFallbackStrategy() : properties.getFallbackStrategy())
                .limit(config.getLimit() > 0
                        ? config.getLimit() : properties.getDefaultLimit())
                .windowMs(config.getWindowMs() > 0
                        ? config.getWindowMs() : properties.getDefaultWindowMs())
                .capacity(config.getCapacity() > 0
                        ? config.getCapacity() : properties.getDefaultCapacity())
                .refillRate(config.getRefillRate() > 0
                        ? config.getRefillRate() : properties.getDefaultRefillRate())
                .build();
    }
}
