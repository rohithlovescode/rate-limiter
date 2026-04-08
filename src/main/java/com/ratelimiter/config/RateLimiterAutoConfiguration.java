package com.ratelimiter.config;

import com.ratelimiter.algorithm.local.LocalSlidingWindowAlgorithm;
import com.ratelimiter.algorithm.local.LocalTokenBucketAlgorithm;
import com.ratelimiter.algorithm.redis.RedisSlidingWindowAlgorithm;
import com.ratelimiter.algorithm.redis.RedisTokenBucketAlgorithm;
import com.ratelimiter.metrics.RateLimiterMetrics;
import com.ratelimiter.service.RateLimiterService;
import com.ratelimiter.service.impl.RedisRateLimiterService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties(RateLimiterProperties.class)
@Import(RedisConfig.class)
public class RateLimiterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedisTokenBucketAlgorithm redisTokenBucketAlgorithm(
            org.springframework.data.redis.core.StringRedisTemplate redisTemplate,
            @SuppressWarnings("rawtypes") org.springframework.data.redis.core.script.RedisScript tokenBucketScript,
            RateLimiterProperties properties) {
        return new RedisTokenBucketAlgorithm(redisTemplate, tokenBucketScript, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisSlidingWindowAlgorithm redisSlidingWindowAlgorithm(
            org.springframework.data.redis.core.StringRedisTemplate redisTemplate,
            @SuppressWarnings("rawtypes") org.springframework.data.redis.core.script.RedisScript slidingWindowScript,
            RateLimiterProperties properties) {
        return new RedisSlidingWindowAlgorithm(redisTemplate, slidingWindowScript, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalTokenBucketAlgorithm localTokenBucketAlgorithm() {
        return new LocalTokenBucketAlgorithm();
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalSlidingWindowAlgorithm localSlidingWindowAlgorithm() {
        return new LocalSlidingWindowAlgorithm();
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimiterMetrics rateLimiterMetrics(io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        return new RateLimiterMetrics(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimiterService rateLimiterService(
            RedisTokenBucketAlgorithm redisTokenBucket,
            RedisSlidingWindowAlgorithm redisSlidingWindow,
            LocalTokenBucketAlgorithm localTokenBucket,
            LocalSlidingWindowAlgorithm localSlidingWindow,
            RateLimiterProperties properties,
            RateLimiterMetrics metrics) {
        return new RedisRateLimiterService(
                redisTokenBucket, redisSlidingWindow,
                localTokenBucket, localSlidingWindow,
                properties, metrics);
    }
}
