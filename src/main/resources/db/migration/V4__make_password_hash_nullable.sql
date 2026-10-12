-- V4__make_password_hash_nullable.sql
-- Allow passwordless Google Sign-In accounts by dropping NOT NULL on legacy password_hash

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'users' 
          AND column_name = 'password_hash' 
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
    END IF;
END $$;
