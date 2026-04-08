package com.ratelimiter.domain;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class RateLimitResult {

    /** Whether the request is allowed to proceed. */
    private boolean allowed;

    /** Number of remaining requests/tokens after this call. */
    private long remaining;

    /** The configured limit (window limit or bucket capacity). */
    private long limit;

    /** Milliseconds the client should wait before retrying. 0 if allowed. */
    private long retryAfterMs;

    /** Which algorithm produced this result. */
    private Algorithm algorithm;

    /** The client ID this result applies to. */
    private String clientId;

    /** Timestamp of this decision. */
    private Instant timestamp;

    /** Human-readable reason for denial (null if allowed). */
    private String reason;

    /** Whether this result came from the local fallback (not Redis). */
    private boolean fallback;

    public static RateLimitResult allowed(String clientId, long remaining, long limit, Algorithm algorithm) {
        return RateLimitResult.builder()
                .allowed(true)
                .clientId(clientId)
                .remaining(remaining)
                .limit(limit)
                .retryAfterMs(0L)
                .algorithm(algorithm)
                .timestamp(Instant.now())
                .build();
    }

    public static RateLimitResult denied(String clientId, long limit, Algorithm algorithm,
                                          long retryAfterMs, String reason) {
        return RateLimitResult.builder()
                .allowed(false)
                .clientId(clientId)
                .remaining(0L)
                .limit(limit)
                .retryAfterMs(retryAfterMs)
                .algorithm(algorithm)
                .timestamp(Instant.now())
                .reason(reason)
                .build();
    }
}
