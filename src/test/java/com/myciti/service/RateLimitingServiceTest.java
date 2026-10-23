package com.myciti.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitingServiceTest {

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService();
    }

    @Test
    void tryConsume_withinLimit_allowsRequests() {
        String testIp = "192.168.1.100";
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimitingService.tryConsume(testIp), "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void tryConsume_exceedingLimit_blocksSubsequentRequests() {
        String testIp = "192.168.1.101";
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimitingService.tryConsume(testIp));
        }
        assertFalse(rateLimitingService.tryConsume(testIp), "21st request should be rate-limited");
    }

    @Test
    void tryConsume_differentIps_independentBuckets() {
        String ip1 = "10.0.0.1";
        String ip2 = "10.0.0.2";

        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimitingService.tryConsume(ip1));
        }
        assertFalse(rateLimitingService.tryConsume(ip1));

        // ip2 should still have full quota
        assertTrue(rateLimitingService.tryConsume(ip2));
    }

    @Test
    void tryConsumeImageUpload_enforcesFifteenPerMinuteLimit() {
        String ip = "192.168.1.150";
        for (int i = 0; i < 15; i++) {
            assertTrue(rateLimitingService.tryConsumeImageUpload(ip), "Upload " + (i + 1) + " should succeed");
        }
        assertFalse(rateLimitingService.tryConsumeImageUpload(ip), "16th upload should be rate-limited");
    }

    @Test
    void tryConsumeSearch_enforcesSixtyPerMinuteLimit() {
        String ip = "192.168.1.160";
        for (int i = 0; i < 60; i++) {
            assertTrue(rateLimitingService.tryConsumeSearch(ip), "Search " + (i + 1) + " should succeed");
        }
        assertFalse(rateLimitingService.tryConsumeSearch(ip), "61st search should be rate-limited");
    }

    @Test
    void tryConsumeAdReport_enforcesFivePerHourLimit() {
        String ip = "192.168.1.170";
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimitingService.tryConsumeAdReport(ip), "Report " + (i + 1) + " should succeed");
        }
        assertFalse(rateLimitingService.tryConsumeAdReport(ip), "6th report should be rate-limited");
    }

    @Test
    void evictExpired_keepsMapBounded() {
        rateLimitingService.tryConsume("1.1.1.1");
        rateLimitingService.tryConsume("2.2.2.2");
        assertEquals(2, rateLimitingService.getActiveBucketCount());

        // Without waiting 10 minutes, nothing evicted
        rateLimitingService.evictExpired();
        assertEquals(2, rateLimitingService.getActiveBucketCount());
    }
}
