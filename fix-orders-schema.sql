-- Add missing columns to orders table
ALTER TABLE orders ADD COLUMN IF NOT EXISTS bundle_code VARCHAR(100);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS bundle_name VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_number VARCHAR(50);

-- Create unique index on order_number if it doesn't exist
CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_order_number ON orders(order_number);

-- Update existing rows if needed (optional)
-- UPDATE orders SET bundle_code = 'unknown' WHERE bundle_code IS NULL;
-- UPDATE orders SET order_number = 'ORD-' || id::text WHERE order_number IS NULL;

