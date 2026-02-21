-- Fix all NOT NULL constraints that are causing issues
-- Make package_name nullable (it's an old column, bundle_code/bundle_name are the new ones)
ALTER TABLE orders ALTER COLUMN package_name DROP NOT NULL;

-- Make order_reference nullable (code uses order_number instead)
ALTER TABLE orders ALTER COLUMN order_reference DROP NOT NULL;

