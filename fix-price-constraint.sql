-- Make price nullable (code uses unit_price and total_amount instead)
ALTER TABLE orders ALTER COLUMN price DROP NOT NULL;

