-- V8__geo_ads_and_announcements.sql
-- Add geo-targeted advertising, announcements, in-app notifications, push subscriptions, and coarse user locations

-- 1. Add coarse location columns to users
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'coarse_lat') THEN
        ALTER TABLE users ADD COLUMN coarse_lat DOUBLE PRECISION;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'coarse_lng') THEN
        ALTER TABLE users ADD COLUMN coarse_lng DOUBLE PRECISION;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'area_name') THEN
        ALTER TABLE users ADD COLUMN area_name VARCHAR(100);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'location_updated_at') THEN
        ALTER TABLE users ADD COLUMN location_updated_at TIMESTAMP WITH TIME ZONE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_coarse_coords ON users(coarse_lat, coarse_lng);

-- 2. Web push subscriptions
CREATE TABLE IF NOT EXISTS push_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    endpoint TEXT NOT NULL UNIQUE,
    p256dh VARCHAR(255) NOT NULL,
    auth VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_push_sub_user_id ON push_subscriptions(user_id);

-- 3. Ad Campaigns
CREATE TABLE IF NOT EXISTS ad_campaigns (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    client_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    headline VARCHAR(255),
    target_type VARCHAR(20) NOT NULL DEFAULT 'RADIUS',
    target_lat DOUBLE PRECISION,
    target_lng DOUBLE PRECISION,
    radius_km DOUBLE PRECISION NOT NULL DEFAULT 5.0,
    target_city VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(500),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    impressions_count BIGINT NOT NULL DEFAULT 0,
    clicks_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ad_campaigns_status ON ad_campaigns(status, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_ad_campaigns_coords ON ad_campaigns(target_lat, target_lng);
CREATE INDEX IF NOT EXISTS idx_ad_campaigns_client ON ad_campaigns(client_user_id);

-- 4. Ad Reports (by public users)
CREATE TABLE IF NOT EXISTS ad_reports (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL REFERENCES ad_campaigns(id) ON DELETE CASCADE,
    reporter_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reason VARCHAR(255) NOT NULL,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ad_reports_campaign ON ad_reports(campaign_id);

-- 5. Announcements
CREATE TABLE IF NOT EXISTS announcements (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT REFERENCES listings(id) ON DELETE CASCADE,
    author_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(500) NOT NULL,
    target_type VARCHAR(20) NOT NULL DEFAULT 'RADIUS',
    target_lat DOUBLE PRECISION,
    target_lng DOUBLE PRECISION,
    radius_km DOUBLE PRECISION NOT NULL DEFAULT 5.0,
    target_city VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(500),
    sent_at TIMESTAMP WITH TIME ZONE,
    recipient_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_announcements_status ON announcements(status);
CREATE INDEX IF NOT EXISTS idx_announcements_author ON announcements(author_user_id);

-- 6. In-App Notifications
CREATE TABLE IF NOT EXISTS in_app_notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    announcement_id BIGINT REFERENCES announcements(id) ON DELETE SET NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(500) NOT NULL,
    link_url VARCHAR(255),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_in_app_notif_user ON in_app_notifications(user_id, is_read, created_at DESC);
