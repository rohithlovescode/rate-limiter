package com.ratelimiter.algorithm.local;

import com.ratelimiter.algorithm.RateLimiterAlgorithm;
import com.ratelimiter.domain.Algorithm;
import com.ratelimiter.domain.RateLimitConfig;
import com.ratelimiter.domain.RateLimitResult;
import com.ratelimiter.domain.RateLimitStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class LocalSlidingWindowAlgorithm implements RateLimiterAlgorithm {

    private final ConcurrentHashMap<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    @Override
    public Algorithm getType() {
        return Algorithm.SLIDING_WINDOW;
    }

    @Override
    public RateLimitResult tryAcquire(String clientId, RateLimitConfig config) {
        Deque<Long> window = windows.computeIfAbsent(clientId, k -> new ArrayDeque<>());
        long now         = System.currentTimeMillis();
        long windowStart = now - config.getWindowMs();

        synchronized (window) {
            // Evict expired entries from head
            while (!window.isEmpty() && window.peekFirst() <= windowStart) {
                window.pollFirst();
            }

            if (window.size() < config.getLimit()) {
                window.addLast(now);
                long remaining = config.getLimit() - window.size();
                return RateLimitResult.builder()
                        .allowed(true).clientId(clientId).remaining(remaining)
                        .limit(config.getLimit()).retryAfterMs(0L)
                        .algorithm(Algorithm.SLIDING_WINDOW).timestamp(Instant.now())
                        .fallback(true).build();
            }

            // Oldest entry tells us when a slot opens up
            long retryAfterMs = window.peekFirst() + config.getWindowMs() - now;
            return RateLimitResult.denied(clientId, config.getLimit(),
                    Algorithm.SLIDING_WINDOW, Math.max(0, retryAfterMs),
                    "Rate limit exceeded (local fallback)");
        }
    }

    @Override
    public void reset(String clientId) {
        windows.remove(clientId);
    }

    @Override
    public RateLimitStatus getStatus(String clientId, RateLimitConfig config) {
        Deque<Long> window = windows.get(clientId);
        long now         = System.currentTimeMillis();
        long windowStart = now - config.getWindowMs();
        long count = 0;
        long resetAt = now;

        if (window != null) {
            synchronized (window) {
                count   = window.stream().filter(t -> t > windowStart).count();
                resetAt = window.isEmpty() ? now : window.peekFirst() + config.getWindowMs();
            }
        }

        return RateLimitStatus.builder()
                .clientId(clientId).algorithm(Algorithm.SLIDING_WINDOW)
                .remaining(Math.max(0, config.getLimit() - count))
                .limit(config.getLimit()).resetAtMs(resetAt)
                .limited(count >= config.getLimit()).build();
    }

    @Scheduled(fixedRate = 60_000)
    public void evictStale() {
        long cutoff      = System.currentTimeMillis() - 300_000;
        int  before      = windows.size();
        windows.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                return e.getValue().isEmpty() ||
                       (e.getValue().peekLast() != null && e.getValue().peekLast() < cutoff);
            }
        });
        int removed = before - windows.size();
        if (removed > 0) {
            log.debug("LocalSlidingWindow evicted {} stale entries", removed);
        }
    }
}
