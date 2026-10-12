-- V1__initial_schema.sql: Initial schema for myCiti listings and images

CREATE TABLE IF NOT EXISTS listings (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    address VARCHAR(500) NOT NULL,
    landmark VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    daily_open_time TIME NOT NULL,
    daily_close_time TIME NOT NULL,
    offer_text VARCHAR(500),
    contact_phone VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS listing_images (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    url VARCHAR(1000) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient queries
CREATE INDEX idx_listings_status_dates ON listings(status, start_date, end_date);
CREATE INDEX idx_listings_category ON listings(category);
CREATE INDEX idx_listings_coords ON listings(latitude, longitude);
CREATE INDEX idx_listing_images_listing_id ON listing_images(listing_id, sort_order);
