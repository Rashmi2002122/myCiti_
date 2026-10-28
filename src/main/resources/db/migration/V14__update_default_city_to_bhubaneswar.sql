-- V14__update_default_city_to_bhubaneswar.sql
-- Update default city from Kolkata to Bhubaneswar for Odisha market launch

ALTER TABLE listings ALTER COLUMN city SET DEFAULT 'Bhubaneswar';
ALTER TABLE submissions ALTER COLUMN city SET DEFAULT 'Bhubaneswar';
