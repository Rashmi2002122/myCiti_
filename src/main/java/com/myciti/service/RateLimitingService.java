package com.myciti.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitingService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingService.class);
    private static final int MAX_ENTRIES = 5000;
    private static final long EVICTION_THRESHOLD_MS = TimeUnit.MINUTES.toMillis(10);

    private static class BucketEntry {
        final Bucket bucket;
        volatile long lastAccessTime;

        BucketEntry(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccessTime = System.currentTimeMillis();
        }
    }

    private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();

    private Bucket createNewBucket() {
        // 20 requests per minute per IP
        Bandwidth limit = Bandwidth.builder()
            .capacity(20)
            .refillGreedy(20, Duration.ofMinutes(1))
            .build();
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    public boolean tryConsume(String ip) {
        String key = (ip == null || ip.isBlank()) ? "anonymous" : ip.trim();

        BucketEntry entry = buckets.get(key);
        if (entry == null) {
            if (buckets.size() >= MAX_ENTRIES) {
                evictExpired();
            }
            entry = buckets.computeIfAbsent(key, k -> new BucketEntry(createNewBucket()));
        }

        entry.lastAccessTime = System.currentTimeMillis();
        return entry.bucket.tryConsume(1);
    }

    /**
     * Periodically evict idle rate limit buckets to prevent memory leaks.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void evictExpired() {
        long cutoff = System.currentTimeMillis() - EVICTION_THRESHOLD_MS;
        int before = buckets.size();
        buckets.entrySet().removeIf(e -> e.getValue().lastAccessTime < cutoff);
        int after = buckets.size();
        if (before > after) {
            log.debug("[RateLimiter] Evicted {} expired IP rate limit buckets (remaining: {})", (before - after), after);
        }
    }

    public int getActiveBucketCount() {
        return buckets.size();
    }
}
