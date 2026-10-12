-- V6__add_city_column_to_listings_and_submissions.sql
-- Add city column to listings and submissions for pan-India multi-city support

DO $$
BEGIN
    -- 1. Add city to listings if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'listings' AND column_name = 'city'
    ) THEN
        ALTER TABLE listings ADD COLUMN city VARCHAR(100) DEFAULT 'Kolkata';
    END IF;

    -- 2. Add city to submissions if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'submissions' AND column_name = 'city'
    ) THEN
        ALTER TABLE submissions ADD COLUMN city VARCHAR(100) DEFAULT 'Kolkata';
    END IF;

    -- 3. Create index on city for listings
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_listings_city'
    ) THEN
        CREATE INDEX idx_listings_city ON listings(city);
    END IF;

    -- 4. Create index on city for submissions
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_submissions_city'
    ) THEN
        CREATE INDEX idx_submissions_city ON submissions(city);
    END IF;
END $$;
