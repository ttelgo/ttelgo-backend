-- Update esims table to match entity schema
-- Add missing columns

ALTER TABLE esims 
  ADD COLUMN IF NOT EXISTS esim_uuid VARCHAR(255) UNIQUE,
  ADD COLUMN IF NOT EXISTS bundle_id VARCHAR(255),
  ADD COLUMN IF NOT EXISTS bundle_name VARCHAR(255),
  ADD COLUMN IF NOT EXISTS matching_id VARCHAR(255) UNIQUE,
  ADD COLUMN IF NOT EXISTS iccid VARCHAR(255) UNIQUE,
  ADD COLUMN IF NOT EXISTS smdp_address VARCHAR(255),
  ADD COLUMN IF NOT EXISTS data_amount INTEGER,
  ADD COLUMN IF NOT EXISTS data_used INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS duration_days INTEGER,
  ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP,
  ADD COLUMN IF NOT EXISTS country_iso VARCHAR(255),
  ADD COLUMN IF NOT EXISTS country_name VARCHAR(255),
  ADD COLUMN IF NOT EXISTS esimgo_order_id VARCHAR(255);

-- Rename existing columns to match entity if they exist with different names
DO $$
BEGIN
  -- Rename esim_go_id to matching_id if it exists and matching_id doesn't
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='esims' AND column_name='esim_go_id')
     AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='esims' AND column_name='matching_id') THEN
    ALTER TABLE esims RENAME COLUMN esim_go_id TO matching_id;
  END IF;
  
  -- Rename qr_code to activation_code if needed (keep both for compatibility)
  -- activation_code already exists, so we'll keep both
END $$;


