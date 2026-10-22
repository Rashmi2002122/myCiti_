package com.myciti.event;

/**
 * Event published when a user's status, role, or profile is updated.
 * Allows security caches (such as JwtAuthenticationFilter) to immediately evict stale user state.
 */
public record UserStatusChangedEvent(Long userId) {}
