-- Reset Admin Password Script
-- This will reset the password for admin@ttelgo.com to: Admin@123456
-- Or create the user if it doesn't exist

-- Option 1: Reset password for existing admin user
-- Replace 'admin@ttelgo.com' with your admin email if different
UPDATE users 
SET 
    password_hash = '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', -- Password: Admin@123456
    role = 'ADMIN',
    updated_at = NOW()
WHERE email = 'admin@ttelgo.com';

-- Option 2: Create or update admin user (if doesn't exist)
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
    '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', -- Password: Admin@123456
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
    updated_at = NOW();

-- Verify the user
SELECT id, email, name, role, is_email_verified, created_at 
FROM users 
WHERE email = 'admin@ttelgo.com';


