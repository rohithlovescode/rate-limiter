package com.ratelimiter.domain;

public enum Algorithm {
    /**
     * Token Bucket: allows bursts up to capacity, refills at a steady rate.
     * Good for: APIs that tolerate short bursts.
     * Config fields used: capacity, refillRate.
     */
    TOKEN_BUCKET,

    /**
     * Sliding Window: counts requests in a rolling time window.
     * Good for: strict per-second/minute rate limits without burst tolerance.
     * Config fields used: limit, windowMs.
     */
    SLIDING_WINDOW
}
