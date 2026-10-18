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

    private String normalizeIp(String ip) {
        return (ip == null || ip.isBlank()) ? "anonymous" : ip.trim();
    }

    private Bucket createBucket(int capacity, Duration refillPeriod) {
        Bandwidth limit = Bandwidth.builder()
            .capacity(capacity)
            .refillGreedy(capacity, refillPeriod)
            .build();
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    private boolean tryConsumeKey(String key, int capacity, Duration refillPeriod) {
        BucketEntry entry = buckets.get(key);
        if (entry == null) {
            if (buckets.size() >= MAX_ENTRIES) {
                evictExpired();
            }
            entry = buckets.computeIfAbsent(key, k -> new BucketEntry(createBucket(capacity, refillPeriod)));
        }

        entry.lastAccessTime = System.currentTimeMillis();
        return entry.bucket.tryConsume(1);
    }

    /**
     * Rate limit for auth endpoints: 20 requests per minute.
     */
    public boolean tryConsumeAuth(String ip) {
        return tryConsumeKey("auth:" + normalizeIp(ip), 20, Duration.ofMinutes(1));
    }

    /**
     * Rate limit for ad impression and click recording: 60 requests per minute.
     */
    public boolean tryConsumeAdAction(String ip) {
        return tryConsumeKey("ad:" + normalizeIp(ip), 60, Duration.ofMinutes(1));
    }

    /**
     * General consume method (defaults to auth rate limit).
     */
    public boolean tryConsume(String ip) {
        return tryConsumeAuth(ip);
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
