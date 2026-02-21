-- Add provider and provider_id columns to users table for OAuth authentication
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS provider VARCHAR(50),
ADD COLUMN IF NOT EXISTS provider_id VARCHAR(255);

-- Create index on provider_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_provider_id ON users(provider_id) WHERE provider_id IS NOT NULL AND deleted_at IS NULL;

-- Create index on provider for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_provider ON users(provider) WHERE provider IS NOT NULL AND deleted_at IS NULL;










