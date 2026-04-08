package com.ratelimiter.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitConfig {

    /** Identifies the entity being rate-limited (user ID, API key, IP address, etc.) */
    private String clientId;

    /** Which algorithm to use. Null = use application default. */
    private Algorithm algorithm;

    /** [SLIDING_WINDOW] Maximum requests allowed per windowMs. */
    private int limit;

    /** [SLIDING_WINDOW] Rolling window size in milliseconds. */
    private int windowMs;

    /** [TOKEN_BUCKET] Maximum number of tokens (burst capacity). */
    private int capacity;

    /** [TOKEN_BUCKET] Rate at which tokens are added per second. */
    private int refillRate;

    /** What to do when Redis is unavailable. Null = use application default. */
    private FallbackStrategy fallbackStrategy;
}
