-- Fix order status values to match Java enum (uppercase)

-- Update all lowercase status values to uppercase
UPDATE orders SET status = 'PENDING' WHERE LOWER(status) = 'pending';
UPDATE orders SET status = 'PROCESSING' WHERE LOWER(status) = 'processing';
UPDATE orders SET status = 'COMPLETED' WHERE LOWER(status) = 'completed';
UPDATE orders SET status = 'CANCELLED' WHERE LOWER(status) = 'cancelled';
UPDATE orders SET status = 'REFUNDED' WHERE LOWER(status) = 'refunded';
UPDATE orders SET status = 'FAILED' WHERE LOWER(status) = 'failed';

-- Update payment_status as well
UPDATE orders SET payment_status = 'PENDING' WHERE LOWER(payment_status) = 'pending';
UPDATE orders SET payment_status = 'SUCCESS' WHERE LOWER(payment_status) = 'success';
UPDATE orders SET payment_status = 'FAILED' WHERE LOWER(payment_status) = 'failed';
UPDATE orders SET payment_status = 'REFUNDED' WHERE LOWER(payment_status) = 'refunded';

-- Verify the changes
SELECT status, payment_status, COUNT(*) as count 
FROM orders 
GROUP BY status, payment_status;

