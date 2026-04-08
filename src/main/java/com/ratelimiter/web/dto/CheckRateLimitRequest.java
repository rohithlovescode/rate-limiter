package com.ratelimiter.web.dto;

import com.ratelimiter.domain.Algorithm;
import com.ratelimiter.domain.FallbackStrategy;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckRateLimitRequest {

    /** Required. The entity being rate-limited. */
    @NotBlank(message = "clientId is required")
    private String clientId;

    /** Optional. Defaults to application config. */
    private Algorithm algorithm;

    /** Optional. For SLIDING_WINDOW. Default: from application.yml */
    private Integer limit;

    /** Optional. For SLIDING_WINDOW. Default: from application.yml */
    private Integer windowMs;

    /** Optional. For TOKEN_BUCKET. Default: from application.yml */
    private Integer capacity;

    /** Optional. For TOKEN_BUCKET. Default: from application.yml */
    private Integer refillRate;

    /** Optional. AP or CP. Default: from application.yml */
    private FallbackStrategy fallbackStrategy;
}
