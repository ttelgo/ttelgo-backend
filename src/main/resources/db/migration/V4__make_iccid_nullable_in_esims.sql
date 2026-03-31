-- V4: Make esims.iccid nullable
-- Reason: eSIMGo API does not always return iccid in the immediate order creation response.
-- The matchingId is used as the primary eSIM identifier for QR code lookups.

-- Step 1: Drop the NOT NULL constraint on iccid
ALTER TABLE esims ALTER COLUMN iccid DROP NOT NULL;

-- Step 2: Increase iccid column length to accommodate all ICCID formats (up to 22 chars)
ALTER TABLE esims ALTER COLUMN iccid TYPE VARCHAR(50);

-- Step 3: Drop the old unconditional unique constraint on iccid
ALTER TABLE esims DROP CONSTRAINT IF EXISTS esims_iccid_key;

-- Step 4: Recreate iccid unique index only on non-null, non-deleted rows
DROP INDEX IF EXISTS idx_esims_iccid;
CREATE UNIQUE INDEX idx_esims_iccid ON esims(iccid) WHERE iccid IS NOT NULL AND deleted_at IS NULL;

-- Step 5: Add unique index on matching_id (the primary identifier from eSIMGo)
CREATE UNIQUE INDEX IF NOT EXISTS idx_esims_matching_id_unique ON esims(matching_id) WHERE matching_id IS NOT NULL AND deleted_at IS NULL;
