package com.ratelimiter.service;

import com.ratelimiter.domain.RateLimitConfig;
import com.ratelimiter.domain.RateLimitResult;
import com.ratelimiter.domain.RateLimitStatus;

/**
 * The primary entry point for rate limiting.
 *
 * Library usage:
 *   @Autowired RateLimiterService rateLimiter;
 *   RateLimitResult r = rateLimiter.check("user:42", config);
 *   if (!r.isAllowed()) throw new TooManyRequestsException();
 *
 * This interface is intentionally simple. All fallback, algorithm routing,
 * and Redis failure handling is encapsulated inside the implementation.
 */
public interface RateLimiterService {

    /**
     * Check if client is within their rate limit and consume one token/slot.
     * NEVER throws. Failures go through configured fallback strategy.
     *
     * @param clientId  Any string identifying the caller: user ID, API key, IP, etc.
     * @param config    Algorithm parameters. Null values fall back to application defaults.
     * @return          Result with allowed=true if request should proceed.
     */
    RateLimitResult check(String clientId, RateLimitConfig config);

    /**
     * Check using all application-level defaults from application.yml.
     */
    RateLimitResult check(String clientId);

    /**
     * Inspect current state without consuming a token.
     */
    RateLimitStatus getStatus(String clientId, RateLimitConfig config);

    /**
     * Clear all rate limit state for a client (admin operation).
     */
    void reset(String clientId);
}
