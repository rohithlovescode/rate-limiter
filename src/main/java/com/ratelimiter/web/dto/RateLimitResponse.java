package com.ratelimiter.web.dto;

import com.ratelimiter.domain.Algorithm;
import com.ratelimiter.domain.RateLimitResult;
import com.ratelimiter.domain.RateLimitStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class RateLimitResponse {

    private boolean allowed;
    private String  clientId;
    private long    remaining;
    private long    limit;
    private long    retryAfterMs;
    private Algorithm algorithm;
    private Instant   timestamp;
    private String    reason;
    private boolean   fallback;

    public static RateLimitResponse from(RateLimitResult result) {
        return RateLimitResponse.builder()
                .allowed(result.isAllowed())
                .clientId(result.getClientId())
                .remaining(result.getRemaining())
                .limit(result.getLimit())
                .retryAfterMs(result.getRetryAfterMs())
                .algorithm(result.getAlgorithm())
                .timestamp(result.getTimestamp())
                .reason(result.getReason())
                .fallback(result.isFallback())
                .build();
    }

    public static RateLimitResponse from(RateLimitStatus status) {
        return RateLimitResponse.builder()
                .clientId(status.getClientId())
                .remaining(status.getRemaining())
                .limit(status.getLimit())
                .algorithm(status.getAlgorithm())
                .allowed(!status.isLimited())
                .build();
    }
}
