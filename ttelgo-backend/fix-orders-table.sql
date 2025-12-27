-- Fix orders table - add missing order_reference column
-- This column is required by the Order entity

-- Add the column first (allowing NULL temporarily)
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_reference VARCHAR(255);

-- Update existing rows with a generated UUID if order_reference is NULL
UPDATE orders 
SET order_reference = 'ORD-' || LPAD(id::TEXT, 10, '0') || '-' || TO_CHAR(created_at, 'YYYYMMDD')
WHERE order_reference IS NULL;

-- Now add the NOT NULL constraint
ALTER TABLE orders ALTER COLUMN order_reference SET NOT NULL;

-- Add unique constraint
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'orders_order_reference_unique'
    ) THEN
        ALTER TABLE orders ADD CONSTRAINT orders_order_reference_unique UNIQUE (order_reference);
    END IF;
END $$;

-- Verify the changes
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'orders' AND column_name = 'order_reference';

