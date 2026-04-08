package com.ratelimiter.health;

import com.ratelimiter.config.RateLimiterProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuator.health.AbstractHealthIndicator;
import org.springframework.boot.actuator.health.Health;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRateLimiterHealthIndicator extends AbstractHealthIndicator {

    private final StringRedisTemplate   redisTemplate;
    private final RateLimiterProperties properties;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            // Write + read a probe key to verify Redis round-trip
            String probeKey = properties.getKeyPrefix() + ":health:probe";
            redisTemplate.opsForValue().set(probeKey, "ok");
            String val = redisTemplate.opsForValue().get(probeKey);
            redisTemplate.delete(probeKey);

            if ("ok".equals(val)) {
                builder.up()
                        .withDetail("redis",    "CONNECTED")
                        .withDetail("fallback", properties.getFallbackStrategy().name());
            } else {
                builder.down().withDetail("redis", "PROBE_FAILED");
            }
        } catch (Exception e) {
            builder.down()
                    .withDetail("redis",    "UNREACHABLE")
                    .withDetail("fallback", properties.getFallbackStrategy().name())
                    .withDetail("error",    e.getMessage());
        }
    }
}
