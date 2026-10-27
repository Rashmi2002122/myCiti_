package com.myciti.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * High-performance L1 in-memory cache configuration powered by Caffeine (W-TinyLFU eviction).
 * Replaces simple ConcurrentHashMap to prevent memory bloat and provide TTL-based expiry.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_LISTINGS = "listings";
    public static final String CACHE_LISTING_BY_ID = "listingById";
    public static final String CACHE_ACTIVE_ADS = "activeAds";
    public static final String CACHE_CLIENT_PROFILES = "clientProfiles";
    public static final String CACHE_UNREAD_NOTIFICATIONS = "unreadNotifications";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        // 1. Active Listings Cache: 10 mins TTL, max 1000 items
        CaffeineCache listingsCache = new CaffeineCache(
            CACHE_LISTINGS,
            Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build()
        );

        // 2. Listing By ID Cache: 15 mins TTL, max 2000 items
        CaffeineCache listingByIdCache = new CaffeineCache(
            CACHE_LISTING_BY_ID,
            Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .recordStats()
                .build()
        );

        // 3. Active Geo Ads Cache: 5 mins TTL, max 200 items
        CaffeineCache activeAdsCache = new CaffeineCache(
            CACHE_ACTIVE_ADS,
            Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build()
        );

        // 4. Client Public Profiles Cache: 15 mins TTL, max 500 items
        CaffeineCache clientProfilesCache = new CaffeineCache(
            CACHE_CLIENT_PROFILES,
            Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .recordStats()
                .build()
        );

        // 5. Unread Notification Count Cache: 30 secs TTL, max 3000 items
        CaffeineCache unreadNotificationsCache = new CaffeineCache(
            CACHE_UNREAD_NOTIFICATIONS,
            Caffeine.newBuilder()
                .maximumSize(3000)
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .recordStats()
                .build()
        );

        cacheManager.setCaches(Arrays.asList(
            listingsCache,
            listingByIdCache,
            activeAdsCache,
            clientProfilesCache,
            unreadNotificationsCache
        ));

        return cacheManager;
    }
}
