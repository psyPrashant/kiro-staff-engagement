-- V4__add_password_hash.sql
-- Adds password_hash column to users table and seeds existing users with
-- a BCrypt hash of 'Password1' (cost factor 10) for local development and testing.

-- Add the password_hash column (nullable for backward compatibility)
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

-- Seed password hashes for existing users.
-- Hash corresponds to plaintext 'Password1' with BCrypt cost factor 10.
UPDATE users SET password_hash = '$2a$10$QqECXUNPAZ3.G.3xxJzrpuT3Q/3v3UF9kbJCOB6N6JQIsXh.ubP8S'
WHERE email = 'alice.johnson@psybergate.com';

UPDATE users SET password_hash = '$2a$10$QqECXUNPAZ3.G.3xxJzrpuT3Q/3v3UF9kbJCOB6N6JQIsXh.ubP8S'
WHERE email = 'bob.smith@psybergate.com';

UPDATE users SET password_hash = '$2a$10$QqECXUNPAZ3.G.3xxJzrpuT3Q/3v3UF9kbJCOB6N6JQIsXh.ubP8S'
WHERE email = 'carol.williams@psybergate.com';
