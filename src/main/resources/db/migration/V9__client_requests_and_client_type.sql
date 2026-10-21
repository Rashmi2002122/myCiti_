-- V9__client_requests_and_client_type.sql
-- Transition account_type to client_type, add client_requests table, and user_saved_listings table

-- 1. Ensure existing SUBMITTER users become CLIENT (if not already done by V7)
UPDATE users SET role = 'CLIENT' WHERE role = 'SUBMITTER';

-- 2. Rename account_type to client_type if account_type exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'account_type'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'client_type'
    ) THEN
        ALTER TABLE users RENAME COLUMN account_type TO client_type;
    ELSIF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' AND column_name = 'client_type'
    ) THEN
        ALTER TABLE users ADD COLUMN client_type VARCHAR(50);
    END IF;
END $$;

-- 3. Create client_requests table
CREATE TABLE IF NOT EXISTS client_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_type VARCHAR(50) NOT NULL,
    business_name VARCHAR(255),
    contact_phone VARCHAR(50) NOT NULL,
    note TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decline_reason VARCHAR(500),
    reviewed_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Enforce only one open (PENDING) request per user
CREATE UNIQUE INDEX IF NOT EXISTS idx_client_requests_one_pending_per_user 
    ON client_requests (user_id) 
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_client_requests_status ON client_requests(status);
CREATE INDEX IF NOT EXISTS idx_client_requests_user_id ON client_requests(user_id);

-- 4. Create user_saved_listings table for bookmarking / saving listings
CREATE TABLE IF NOT EXISTS user_saved_listings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_saved_listing UNIQUE (user_id, listing_id)
);

CREATE INDEX IF NOT EXISTS idx_user_saved_listings_user ON user_saved_listings(user_id);
CREATE INDEX IF NOT EXISTS idx_user_saved_listings_listing ON user_saved_listings(listing_id);
