-- Update payment_status constraint to allow SUCCEEDED (code uses SUCCEEDED, DB expects SUCCESS)
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_payment_status_check;
ALTER TABLE orders ADD CONSTRAINT orders_payment_status_check 
    CHECK (payment_status::text = ANY (ARRAY['PENDING'::character varying, 'SUCCESS'::character varying, 'SUCCEEDED'::character varying, 'FAILED'::character varying, 'REFUNDED'::character varying, 'PARTIALLY_REFUNDED'::character varying]::text[]));

