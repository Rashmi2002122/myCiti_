-- V10__client_profiles_and_reports.sql
-- Create client_profiles, profile_reports, and add client_profile_id to listings

-- 1. Create client_profiles table
CREATE TABLE IF NOT EXISTS client_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    slug VARCHAR(120) NOT NULL UNIQUE,
    business_name VARCHAR(255) NOT NULL,
    client_type VARCHAR(50) NOT NULL,
    bio VARCHAR(160),
    logo_url VARCHAR(500),
    cover_url VARCHAR(500),
    address VARCHAR(500),
    landmark VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    public_phone VARCHAR(50),
    show_phone BOOLEAN NOT NULL DEFAULT FALSE,
    whatsapp_number VARCHAR(50),
    daily_open_time TIME,
    daily_close_time TIME,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMP WITH TIME ZONE,
    verified_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_client_profiles_slug ON client_profiles(slug);
CREATE INDEX IF NOT EXISTS idx_client_profiles_user_id ON client_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_client_profiles_status ON client_profiles(status);
CREATE INDEX IF NOT EXISTS idx_client_profiles_verified ON client_profiles(verified);

-- 2. Create profile_reports table
CREATE TABLE IF NOT EXISTS profile_reports (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL REFERENCES client_profiles(id) ON DELETE CASCADE,
    reporter_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reason VARCHAR(50) NOT NULL,
    details VARCHAR(300),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resolved_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_profile_reports_profile_id ON profile_reports(profile_id);
CREATE INDEX IF NOT EXISTS idx_profile_reports_status ON profile_reports(status);

-- 3. Add client_profile_id to listings table
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'listings' AND column_name = 'client_profile_id'
    ) THEN
        ALTER TABLE listings ADD COLUMN client_profile_id BIGINT REFERENCES client_profiles(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_listings_client_profile_id ON listings(client_profile_id);

-- 4. Backfill existing CLIENT users into client_profiles
INSERT INTO client_profiles (
    user_id,
    slug,
    business_name,
    client_type,
    status,
    verified,
    show_phone,
    created_at,
    updated_at
)
SELECT 
    u.id,
    LOWER(REGEXP_REPLACE(REGEXP_REPLACE(COALESCE(NULLIF(TRIM(u.business_name), ''), NULLIF(TRIM(u.name), ''), 'client-' || u.id), '[^a-zA-Z0-9]+', '-', 'g'), '^-+|-+$', '', 'g')) || '-' || u.id,
    COALESCE(NULLIF(TRIM(u.business_name), ''), NULLIF(TRIM(u.name), ''), 'Client ' || u.id),
    COALESCE(NULLIF(TRIM(u.client_type), ''), 'STORE'),
    'ACTIVE',
    FALSE,
    FALSE,
    u.created_at,
    u.updated_at
FROM users u
WHERE u.role = 'CLIENT'
  AND NOT EXISTS (SELECT 1 FROM client_profiles cp WHERE cp.user_id = u.id);

-- 5. Backfill listings to link to client_profiles of submitting users
UPDATE listings l
SET client_profile_id = cp.id
FROM submissions s
JOIN client_profiles cp ON cp.user_id = s.user_id
WHERE l.source_submission_id = s.id
  AND l.client_profile_id IS NULL;
