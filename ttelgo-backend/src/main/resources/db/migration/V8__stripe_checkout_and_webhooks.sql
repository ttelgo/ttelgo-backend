-- Add Stripe Checkout specific columns to payments table
ALTER TABLE payments ADD COLUMN IF NOT EXISTS stripe_session_id VARCHAR(255);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS customer_email VARCHAR(255);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS currency VARCHAR(10);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS payment_intent_id VARCHAR(255);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Make sure we can look up by session id quickly
CREATE UNIQUE INDEX IF NOT EXISTS idx_payments_stripe_session_id ON payments(stripe_session_id);
CREATE INDEX IF NOT EXISTS idx_payments_payment_intent_id ON payments(payment_intent_id);

-- Stripe webhook idempotency table
CREATE TABLE IF NOT EXISTS stripe_webhook_events (
    event_id VARCHAR(255) PRIMARY KEY,
    received_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

