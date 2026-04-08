package com.ratelimiter.algorithm.local;

import com.ratelimiter.algorithm.RateLimiterAlgorithm;
import com.ratelimiter.domain.Algorithm;
import com.ratelimiter.domain.RateLimitConfig;
import com.ratelimiter.domain.RateLimitResult;
import com.ratelimiter.domain.RateLimitStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class LocalTokenBucketAlgorithm implements RateLimiterAlgorithm {

    // State per client ID
    private final ConcurrentHashMap<String, BucketState> buckets = new ConcurrentHashMap<>();

    @Override
    public Algorithm getType() {
        return Algorithm.TOKEN_BUCKET;
    }

    @Override
    public RateLimitResult tryAcquire(String clientId, RateLimitConfig config) {
        BucketState bucket = buckets.computeIfAbsent(clientId,
                k -> new BucketState(config.getCapacity()));

        synchronized (bucket) {
            bucket.refill(config.getRefillRate(), config.getCapacity());

            if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0;
                long remaining = (long) Math.floor(bucket.tokens);
                return RateLimitResult.builder()
                        .allowed(true).clientId(clientId).remaining(remaining)
                        .limit(config.getCapacity()).retryAfterMs(0L)
                        .algorithm(Algorithm.TOKEN_BUCKET).timestamp(Instant.now())
                        .fallback(true).build();
            }

            long retryAfterMs = (long) Math.ceil(1000.0 / config.getRefillRate());
            return RateLimitResult.denied(clientId, config.getCapacity(),
                    Algorithm.TOKEN_BUCKET, retryAfterMs, "Rate limit exceeded (local fallback)");
        }
    }

    @Override
    public void reset(String clientId) {
        buckets.remove(clientId);
    }

    @Override
    public RateLimitStatus getStatus(String clientId, RateLimitConfig config) {
        BucketState bucket = buckets.get(clientId);
        long remaining = bucket != null ? (long) Math.floor(bucket.tokens) : config.getCapacity();
        return RateLimitStatus.builder()
                .clientId(clientId).algorithm(Algorithm.TOKEN_BUCKET)
                .remaining(remaining).limit(config.getCapacity())
                .resetAtMs(System.currentTimeMillis())
                .limited(remaining < 1).build();
    }

    /** Evict entries that haven't been accessed in 5 minutes to prevent memory leak. */
    @Scheduled(fixedRate = 60_000)
    public void evictStale() {
        long cutoff = System.currentTimeMillis() - 300_000;
        int before  = buckets.size();
        buckets.entrySet().removeIf(e -> e.getValue().lastAccessMs < cutoff);
        int removed = before - buckets.size();
        if (removed > 0) {
            log.debug("LocalTokenBucket evicted {} stale entries", removed);
        }
    }

    private static class BucketState {
        double tokens;
        long   lastRefillMs;
        long   lastAccessMs;

        BucketState(int capacity) {
            this.tokens       = capacity;
            this.lastRefillMs = System.currentTimeMillis();
            this.lastAccessMs = this.lastRefillMs;
        }

        void refill(int refillRate, int capacity) {
            long   now           = System.currentTimeMillis();
            long   elapsed       = now - lastRefillMs;
            double tokensToAdd   = (elapsed / 1000.0) * refillRate;
            tokens               = Math.min(capacity, tokens + tokensToAdd);
            lastRefillMs         = now;
            lastAccessMs         = now;
        }
    }
}
