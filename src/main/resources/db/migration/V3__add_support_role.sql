-- Add SUPPORT role to user_role enum for internal TikTel staff
-- This allows support staff to have limited admin access

-- PostgreSQL allows adding values to ENUM types
ALTER TYPE user_role ADD VALUE IF NOT EXISTS 'SUPPORT';

-- Note: IF NOT EXISTS is available in PostgreSQL 9.5+
-- If using older version, use: ALTER TYPE user_role ADD VALUE 'SUPPORT';

