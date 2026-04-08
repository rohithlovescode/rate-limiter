package com.ratelimiter.domain;

public enum FallbackStrategy {
    /**
     * AP — Available + Partition Tolerant.
     * When Redis is unreachable, fall back to local in-memory rate limiting.
     * Each service instance tracks independently — may admit up to N*limit RPS
     * across N instances during partition. Prefer for user-facing, non-critical endpoints.
     */
    AP,

    /**
     * CP — Consistent + Partition Tolerant.
     * When Redis is unreachable, deny ALL requests (fail-closed).
     * Guarantees the rate limit is never exceeded globally.
     * Prefer for billing, security, or abuse-prevention endpoints.
     */
    CP
}
