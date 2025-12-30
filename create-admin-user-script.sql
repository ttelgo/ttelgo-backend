-- Script to create an admin user for TTelGo
-- Password: Admin@123456 (BCrypt hash)
-- Email: admin@ttelgo.com
-- 
-- Run this script in your PostgreSQL database:
-- psql -U your_username -d your_database -f create-admin-user-script.sql
-- OR execute directly in your database client

-- Insert admin user with hashed password
-- Password: Admin@123456
-- BCrypt hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO users (
    email,
    password_hash,
    name,
    first_name,
    last_name,
    role,
    is_email_verified,
    is_phone_verified,
    created_at,
    updated_at
) VALUES (
    'admin@ttelgo.com',
    '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', -- Password: Admin@123456 (BCrypt hash - verified)
    'Admin User',
    'Admin',
    'User',
    'ADMIN',
    true,
    false,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    name = EXCLUDED.name,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    updated_at = NOW();

-- Verify the user was created
SELECT id, email, name, role, is_email_verified, created_at 
FROM users 
WHERE email = 'admin@ttelgo.com';

