package com.ratelimiter.web;

import com.ratelimiter.domain.*;
import com.ratelimiter.service.RateLimiterService;
import com.ratelimiter.web.dto.CheckRateLimitRequest;
import com.ratelimiter.web.dto.RateLimitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rate-limit")
@RequiredArgsConstructor
@Tag(name = "Rate Limiter", description = "Distributed rate limiting API")
public class RateLimiterController {

    private final RateLimiterService rateLimiterService;

    /**
     * Primary endpoint: check if a request is allowed.
     * Returns 200 if allowed, 429 if rate-limited.
     * Always includes standard rate-limit response headers.
     */
    @PostMapping("/check")
    @Operation(summary = "Check rate limit", description =
            "Consumes one token/slot for clientId. Returns 200 if allowed, 429 if limited.")
    public ResponseEntity<RateLimitResponse> check(
            @Valid @RequestBody CheckRateLimitRequest request) {

        RateLimitConfig config = RateLimitConfig.builder()
                .algorithm(request.getAlgorithm())
                .limit(request.getLimit()     != null ? request.getLimit()      : 0)
                .windowMs(request.getWindowMs() != null ? request.getWindowMs() : 0)
                .capacity(request.getCapacity() != null ? request.getCapacity() : 0)
                .refillRate(request.getRefillRate() != null ? request.getRefillRate() : 0)
                .fallbackStrategy(request.getFallbackStrategy())
                .build();

        RateLimitResult result   = rateLimiterService.check(request.getClientId(), config);
        RateLimitResponse body   = RateLimitResponse.from(result);
        HttpStatus status        = result.isAllowed() ? HttpStatus.OK : HttpStatus.TOO_MANY_REQUESTS;

        return ResponseEntity.status(status)
                .header("X-RateLimit-Limit",     String.valueOf(result.getLimit()))
                .header("X-RateLimit-Remaining", String.valueOf(result.getRemaining()))
                .header("X-RateLimit-Algorithm", result.getAlgorithm().name())
                .header("X-RateLimit-Fallback",  String.valueOf(result.isFallback()))
                .header("Retry-After",
                        result.getRetryAfterMs() > 0
                                ? String.valueOf(result.getRetryAfterMs() / 1000)
                                : "0")
                .body(body);
    }

    /**
     * Read-only status check — does NOT consume a token.
     */
    @GetMapping("/status/{clientId}")
    @Operation(summary = "Get status", description = "Inspect current rate limit state without consuming.")
    public ResponseEntity<RateLimitResponse> getStatus(
            @PathVariable String clientId,
            @RequestParam(defaultValue = "SLIDING_WINDOW") Algorithm algorithm,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "60000") int windowMs,
            @RequestParam(defaultValue = "100") int capacity,
            @RequestParam(defaultValue = "10") int refillRate) {

        RateLimitConfig config = RateLimitConfig.builder()
                .algorithm(algorithm).limit(limit).windowMs(windowMs)
                .capacity(capacity).refillRate(refillRate).build();

        RateLimitStatus status = rateLimiterService.getStatus(clientId, config);
        return ResponseEntity.ok(RateLimitResponse.from(status));
    }

    /**
     * Admin: reset rate limit state for a client.
     */
    @DeleteMapping("/reset/{clientId}")
    @Operation(summary = "Reset client", description = "Clears all rate limit state for clientId.")
    public ResponseEntity<Void> reset(@PathVariable String clientId) {
        rateLimiterService.reset(clientId);
        return ResponseEntity.noContent().build();
    }
}
