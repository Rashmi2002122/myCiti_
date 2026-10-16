-- V7__migrate_roles_to_client_and_user.sql
-- Transition access model from 2 roles to 3 roles:
-- 1. ADMIN (unchanged)
-- 2. CLIENT (renamed from SUBMITTER - store, company, business owners who post listings)
-- 3. USER (new role for general public - read-only consumer)

UPDATE users SET role = 'CLIENT' WHERE role = 'SUBMITTER' OR role IS NULL;

ALTER TABLE users ALTER COLUMN role SET DEFAULT 'USER';
