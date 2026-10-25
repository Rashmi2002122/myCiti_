-- V12__performance_indexes.sql
-- High-concurrency performance indexes to support 1000+ simultaneous users

-- 1. Index for fast cleanup of expired/revoked refresh tokens
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- 2. Index for fast retention pruning of old in-app notifications
CREATE INDEX IF NOT EXISTS idx_in_app_notif_created_at ON in_app_notifications(created_at);

-- 3. Composite index on active published listings for city browsing
CREATE INDEX IF NOT EXISTS idx_listings_city_status ON listings(city, status);

-- 4. Fast lookup for sent announcements
CREATE INDEX IF NOT EXISTS idx_announcements_sent_at ON announcements(sent_at);
