-- V13__backfill_client_profile_listings.sql
-- Ensure all listings originating from a client user's submissions are linked to their client profile

UPDATE listings l
SET client_profile_id = cp.id
FROM submissions s
JOIN client_profiles cp ON cp.user_id = s.user_id
WHERE l.source_submission_id = s.id
  AND (l.client_profile_id IS NULL OR l.client_profile_id != cp.id);
