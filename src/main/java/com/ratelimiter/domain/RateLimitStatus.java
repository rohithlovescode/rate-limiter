package com.ratelimiter.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RateLimitStatus {

    private String clientId;
    private Algorithm algorithm;

    /** Requests or tokens remaining right now. */
    private long remaining;

    /** Configured limit. */
    private long limit;

    /** Unix epoch milliseconds when the window resets or bucket is full. */
    private long resetAtMs;

    /** True if the client is currently rate-limited. */
    private boolean limited;
}
