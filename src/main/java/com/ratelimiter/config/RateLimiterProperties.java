package com.ratelimiter.config;

import com.ratelimiter.domain.Algorithm;
import com.ratelimiter.domain.FallbackStrategy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
@Validated
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    @NotNull
    private Algorithm defaultAlgorithm = Algorithm.SLIDING_WINDOW;

    @NotNull
    private FallbackStrategy fallbackStrategy = FallbackStrategy.AP;

    @Min(1)
    private int defaultLimit = 100;

    @Min(1000)
    private int defaultWindowMs = 60000;

    @Min(1)
    private int defaultCapacity = 100;

    @Min(1)
    private int defaultRefillRate = 10;

    private String keyPrefix = "rl";
}
