-- Add missing activated_at column to esims table
ALTER TABLE esims ADD COLUMN IF NOT EXISTS activated_at TIMESTAMP;


