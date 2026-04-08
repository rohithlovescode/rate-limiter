package com.ratelimiter.algorithm;

import com.ratelimiter.domain.Algorithm;
import com.ratelimiter.domain.RateLimitConfig;
import com.ratelimiter.domain.RateLimitResult;
import com.ratelimiter.domain.RateLimitStatus;

public interface RateLimiterAlgorithm {

    Algorithm getType();

    /**
     * Attempt to consume one token / count one request for the given client.
     * MUST be thread-safe.
     */
    RateLimitResult tryAcquire(String clientId, RateLimitConfig config);

    /** Delete all state for a client (admin reset). */
    void reset(String clientId);

    /** Inspect current state without consuming. */
    RateLimitStatus getStatus(String clientId, RateLimitConfig config);
}
