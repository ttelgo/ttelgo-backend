-- Script to create or update a user with ADMIN role
-- Replace 'your-email@example.com' with the email you used to register

-- Option 1: Update existing user to ADMIN role
UPDATE users 
SET role = 'ADMIN' 
WHERE email = 'your-email@example.com';

-- Option 2: Create a new admin user directly (if you prefer)
-- Note: You'll still need to go through OTP verification via the frontend
-- But this creates the user with ADMIN role from the start
-- INSERT INTO users (email, role, is_email_verified, created_at, updated_at)
-- VALUES ('admin@example.com', 'ADMIN', true, NOW(), NOW());

-- Verify the update
SELECT id, email, role FROM users WHERE email = 'your-email@example.com';

