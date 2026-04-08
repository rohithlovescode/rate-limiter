package com.ratelimiter.metrics;

import com.ratelimiter.domain.RateLimitResult;
import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RateLimiterMetrics {

    private final Counter allowedCounter;
    private final Counter deniedCounter;
    private final Counter redisErrorCounter;
    private final Counter fallbackCounter;

    public RateLimiterMetrics(MeterRegistry registry) {
        this.allowedCounter = Counter.builder("rate_limiter_requests_total")
                .tag("result", "allowed")
                .description("Total allowed requests")
                .register(registry);

        this.deniedCounter = Counter.builder("rate_limiter_requests_total")
                .tag("result", "denied")
                .description("Total denied requests")
                .register(registry);

        this.redisErrorCounter = Counter.builder("rate_limiter_redis_errors_total")
                .description("Total Redis connectivity errors")
                .register(registry);

        this.fallbackCounter = Counter.builder("rate_limiter_fallback_activations_total")
                .description("Total fallback activations (Redis unavailable)")
                .register(registry);
    }

    public void record(RateLimitResult result, boolean isFallback) {
        if (result.isAllowed()) {
            allowedCounter.increment();
        } else {
            deniedCounter.increment();
        }
        if (isFallback) {
            fallbackCounter.increment();
        }
    }

    public void recordRedisError() {
        redisErrorCounter.increment();
    }
}
