-- V5__submission_alignment_and_listing_source.sql
-- Align submissions table schema and add source_submission_id to listings

CREATE TABLE IF NOT EXISTS submissions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL DEFAULT '',
    description TEXT NOT NULL,
    venue_name VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    landmark VARCHAR(255) NOT NULL,
    city VARCHAR(100) DEFAULT 'Kolkata',
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    daily_open_time TIME NOT NULL,
    daily_close_time TIME NOT NULL,
    offer_text VARCHAR(500),
    contact_phone VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    decline_reason TEXT,
    reviewed_by VARCHAR(255),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    listing_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS submission_images (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
    url VARCHAR(1000) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    -- 1. Rename event_name to title if title does not exist
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'submissions' AND column_name = 'event_name'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'submissions' AND column_name = 'title'
    ) THEN
        ALTER TABLE submissions RENAME COLUMN event_name TO title;
    END IF;

    -- If neither event_name nor title existed (edge case), add title
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'submissions' AND column_name = 'title'
    ) THEN
        ALTER TABLE submissions ADD COLUMN title VARCHAR(255) NOT NULL DEFAULT '';
    END IF;

    -- 2. Make legacy contact_name and contact_email nullable if present
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'submissions' AND column_name = 'contact_name' AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE submissions ALTER COLUMN contact_name DROP NOT NULL;
    END IF;

    -- 3. Add source_submission_id to listings if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'listings' AND column_name = 'source_submission_id'
    ) THEN
        ALTER TABLE listings ADD COLUMN source_submission_id BIGINT;
        ALTER TABLE listings ADD CONSTRAINT fk_listings_source_submission 
            FOREIGN KEY (source_submission_id) REFERENCES submissions(id) ON DELETE SET NULL;
    END IF;

    -- 4. Ensure index on listings source_submission_id
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_listings_source_sub'
    ) THEN
        CREATE INDEX idx_listings_source_sub ON listings(source_submission_id);
    END IF;

    -- 5. Ensure indexes on submissions
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_submissions_user_status'
    ) THEN
        CREATE INDEX idx_submissions_user_status ON submissions(user_id, status);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_sub_images_sub_id'
    ) THEN
        CREATE INDEX idx_sub_images_sub_id ON submission_images(submission_id, sort_order);
    END IF;
END $$;
