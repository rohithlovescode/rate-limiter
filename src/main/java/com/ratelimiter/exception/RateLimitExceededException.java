package com.ratelimiter.exception;

import com.ratelimiter.domain.RateLimitResult;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitExceededException extends RuntimeException {

    private final RateLimitResult result;

    public RateLimitExceededException(RateLimitResult result) {
        super("Rate limit exceeded for client: " + result.getClientId());
        this.result = result;
    }
}
