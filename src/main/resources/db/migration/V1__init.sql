-- =====================================================
-- TTelGo eSIM Reseller Platform - Initial Schema
-- Version: 1.0
-- Description: Complete database schema for B2C and B2B eSIM platform
-- =====================================================

-- =====================================================
-- ENUMS AND TYPES
-- =====================================================

-- User roles
CREATE TYPE user_role AS ENUM ('CUSTOMER', 'ADMIN', 'SUPER_ADMIN', 'VENDOR_ADMIN', 'VENDOR_SUPPORT');

-- User status
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION');

-- Vendor billing mode
CREATE TYPE billing_mode AS ENUM ('PREPAID', 'POSTPAID');

-- Vendor status
CREATE TYPE vendor_status AS ENUM ('ACTIVE', 'SUSPENDED', 'PENDING_APPROVAL', 'DEACTIVATED');

-- Order status
CREATE TYPE order_status AS ENUM (
    'ORDER_CREATED',
    'PAYMENT_PENDING',
    'PAYMENT_PROCESSING',
    'PAID',
    'PROVISIONING',
    'COMPLETED',
    'FAILED',
    'CANCELED',
    'REFUNDED',
    'PENDING_SYNC',
    'SYNC_FAILED'
);

-- Payment status
CREATE TYPE payment_status AS ENUM (
    'CREATED',
    'REQUIRES_ACTION',
    'PROCESSING',
    'SUCCEEDED',
    'FAILED',
    'CANCELED',
    'REFUNDED',
    'PARTIALLY_REFUNDED'
);

-- Payment type
CREATE TYPE payment_type AS ENUM ('B2C_ORDER', 'VENDOR_TOPUP', 'VENDOR_ORDER');

-- eSIM status
CREATE TYPE esim_status AS ENUM (
    'CREATED',
    'PENDING_ACTIVATION',
    'ACTIVE',
    'SUSPENDED',
    'EXPIRED',
    'TERMINATED',
    'ERROR'
);

-- Ledger entry type
CREATE TYPE ledger_entry_type AS ENUM ('DEBIT', 'CREDIT', 'REFUND', 'ADJUSTMENT', 'REVERSAL');

-- Ledger entry status
CREATE TYPE ledger_entry_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED', 'REVERSED');

-- API key status
CREATE TYPE api_key_status AS ENUM ('ACTIVE', 'REVOKED', 'EXPIRED');

-- KYC status
CREATE TYPE kyc_status AS ENUM ('PENDING', 'SUBMITTED', 'APPROVED', 'REJECTED', 'REQUIRES_UPDATE');

-- Notification type
CREATE TYPE notification_type AS ENUM ('EMAIL', 'SMS', 'PUSH', 'WEBHOOK');

-- Notification status
CREATE TYPE notification_status AS ENUM ('PENDING', 'SENT', 'FAILED', 'DELIVERED', 'BOUNCED');

-- =====================================================
-- CORE TABLES
-- =====================================================

-- Users table (B2C customers and admin users)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(50),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    password_hash VARCHAR(255),
    role user_role NOT NULL DEFAULT 'CUSTOMER',
    status user_status NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    last_login_ip VARCHAR(45),
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT users_email_check CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$')
);

CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_phone ON users(phone_number) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_role ON users(role) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_status ON users(status) WHERE deleted_at IS NULL;

-- Vendors table (B2B resellers)
CREATE TABLE vendors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(50),
    contact_person VARCHAR(255),
    billing_mode billing_mode NOT NULL DEFAULT 'PREPAID',
    status vendor_status NOT NULL DEFAULT 'PENDING_APPROVAL',
    
    -- Financial
    wallet_balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    credit_limit DECIMAL(15, 2) DEFAULT 0.00,
    outstanding_balance DECIMAL(15, 2) DEFAULT 0.00,
    currency VARCHAR(3) DEFAULT 'USD',
    
    -- Limits and controls
    daily_order_limit INT,
    monthly_order_limit INT,
    daily_spend_limit DECIMAL(15, 2),
    monthly_spend_limit DECIMAL(15, 2),
    
    -- Payment terms (for POSTPAID)
    payment_terms VARCHAR(50), -- e.g., "NET_7", "NET_15", "NET_30"
    invoice_cycle_day INT, -- Day of month for invoicing
    
    -- Risk and compliance
    risk_score INT DEFAULT 0,
    is_verified BOOLEAN DEFAULT FALSE,
    kyc_completed BOOLEAN DEFAULT FALSE,
    
    -- Webhook configuration
    webhook_url VARCHAR(500),
    webhook_secret VARCHAR(255),
    webhook_enabled BOOLEAN DEFAULT FALSE,
    
    -- API access
    api_enabled BOOLEAN DEFAULT TRUE,
    
    -- Metadata
    country VARCHAR(2),
    timezone VARCHAR(50) DEFAULT 'UTC',
    notes TEXT,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT vendors_wallet_balance_check CHECK (wallet_balance >= 0),
    CONSTRAINT vendors_credit_limit_check CHECK (credit_limit >= 0),
    CONSTRAINT vendors_outstanding_balance_check CHECK (outstanding_balance >= 0)
);

CREATE INDEX idx_vendors_email ON vendors(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_vendors_status ON vendors(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_vendors_billing_mode ON vendors(billing_mode);

-- Vendor users (admin and support users for each vendor)
CREATE TABLE vendor_users (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role user_role NOT NULL DEFAULT 'VENDOR_SUPPORT',
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    deleted_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(vendor_id, user_id),
    CONSTRAINT vendor_users_role_check CHECK (role IN ('VENDOR_ADMIN', 'VENDOR_SUPPORT'))
);

CREATE INDEX idx_vendor_users_vendor_id ON vendor_users(vendor_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_vendor_users_user_id ON vendor_users(user_id) WHERE deleted_at IS NULL;

-- Orders table (B2C and B2B orders)
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    
    -- Customer/Vendor identification
    user_id BIGINT REFERENCES users(id), -- For B2C orders
    vendor_id BIGINT REFERENCES vendors(id), -- For B2B orders
    customer_email VARCHAR(255), -- Can be different from user email
    
    -- Order details
    bundle_code VARCHAR(100) NOT NULL,
    bundle_name VARCHAR(255),
    quantity INT NOT NULL DEFAULT 1,
    
    -- Pricing
    unit_price DECIMAL(15, 2) NOT NULL,
    total_amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    
    -- eSIM Go reference
    esimgo_order_id VARCHAR(255), -- Reference ID from eSIM Go
    
    -- Status and lifecycle
    status order_status NOT NULL DEFAULT 'ORDER_CREATED',
    payment_status payment_status,
    
    -- Metadata
    country_iso VARCHAR(2),
    data_amount VARCHAR(50), -- e.g., "5GB", "10GB"
    validity_days INT,
    
    -- Tracking
    ip_address VARCHAR(45),
    user_agent TEXT,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP WITH TIME ZONE,
    provisioned_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    canceled_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    -- Error tracking
    error_code VARCHAR(50),
    error_message TEXT,
    retry_count INT DEFAULT 0,
    last_retry_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT orders_quantity_check CHECK (quantity > 0),
    CONSTRAINT orders_amount_check CHECK (total_amount >= 0),
    CONSTRAINT orders_customer_check CHECK (
        (user_id IS NOT NULL AND vendor_id IS NULL) OR 
        (vendor_id IS NOT NULL AND user_id IS NULL)
    )
);

CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_user_id ON orders(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_orders_vendor_id ON orders(vendor_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_orders_status ON orders(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX idx_orders_esimgo_order_id ON orders(esimgo_order_id);

-- Payments table (Stripe payments)
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    payment_number VARCHAR(50) NOT NULL UNIQUE,
    
    -- Type and references
    type payment_type NOT NULL,
    order_id BIGINT REFERENCES orders(id),
    vendor_id BIGINT REFERENCES vendors(id),
    user_id BIGINT REFERENCES users(id),
    
    -- Stripe references
    stripe_payment_intent_id VARCHAR(255) UNIQUE,
    stripe_charge_id VARCHAR(255),
    stripe_refund_id VARCHAR(255),
    
    -- Amount
    amount DECIMAL(15, 2) NOT NULL,
    refunded_amount DECIMAL(15, 2) DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    
    -- Status
    status payment_status NOT NULL DEFAULT 'CREATED',
    
    -- Customer details
    customer_email VARCHAR(255),
    customer_name VARCHAR(255),
    
    -- Payment method
    payment_method_type VARCHAR(50), -- e.g., "card", "ideal", etc.
    payment_method_last4 VARCHAR(10),
    payment_method_brand VARCHAR(50),
    
    -- Metadata (from Stripe)
    metadata JSONB,
    
    -- Idempotency
    idempotency_key VARCHAR(255),
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    succeeded_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    refunded_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Error tracking
    error_code VARCHAR(100),
    error_message TEXT,
    
    CONSTRAINT payments_amount_check CHECK (amount >= 0),
    CONSTRAINT payments_refunded_amount_check CHECK (refunded_amount >= 0 AND refunded_amount <= amount)
);

CREATE INDEX idx_payments_payment_number ON payments(payment_number);
CREATE INDEX idx_payments_stripe_payment_intent_id ON payments(stripe_payment_intent_id);
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_vendor_id ON payments(vendor_id);
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_type ON payments(type);
CREATE INDEX idx_payments_created_at ON payments(created_at DESC);
CREATE INDEX idx_payments_idempotency_key ON payments(idempotency_key);

-- eSIMs table
CREATE TABLE esims (
    id BIGSERIAL PRIMARY KEY,
    
    -- References
    order_id BIGINT NOT NULL REFERENCES orders(id),
    user_id BIGINT REFERENCES users(id),
    vendor_id BIGINT REFERENCES vendors(id),
    
    -- eSIM identifiers
    iccid VARCHAR(30) UNIQUE NOT NULL,
    matching_id VARCHAR(255), -- From eSIM Go
    smdp_address VARCHAR(255),
    activation_code VARCHAR(500), -- LPA:1$... format
    
    -- QR code
    qr_code TEXT,
    qr_code_url VARCHAR(500),
    
    -- Bundle details
    bundle_code VARCHAR(100),
    bundle_name VARCHAR(255),
    
    -- Status and lifecycle
    status esim_status NOT NULL DEFAULT 'CREATED',
    
    -- Usage tracking
    data_limit_bytes BIGINT,
    data_used_bytes BIGINT DEFAULT 0,
    data_remaining_bytes BIGINT,
    
    -- Validity
    valid_from TIMESTAMP WITH TIME ZONE,
    valid_until TIMESTAMP WITH TIME ZONE,
    validity_days INT,
    
    -- Activation
    activated_at TIMESTAMP WITH TIME ZONE,
    activation_code_ios VARCHAR(500),
    activation_code_android VARCHAR(500),
    
    -- Country/Network
    country_iso VARCHAR(2),
    network_name VARCHAR(255),
    
    -- Sync with eSIM Go
    last_synced_at TIMESTAMP WITH TIME ZONE,
    sync_status VARCHAR(50), -- 'SYNCED', 'PENDING', 'FAILED'
    sync_error TEXT,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expired_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_esims_iccid ON esims(iccid) WHERE deleted_at IS NULL;
CREATE INDEX idx_esims_matching_id ON esims(matching_id);
CREATE INDEX idx_esims_order_id ON esims(order_id);
CREATE INDEX idx_esims_user_id ON esims(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_esims_vendor_id ON esims(vendor_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_esims_status ON esims(status);
CREATE INDEX idx_esims_valid_until ON esims(valid_until);

-- =====================================================
-- VENDOR BILLING & LEDGER
-- =====================================================

-- Vendor ledger entries (double-entry bookkeeping)
CREATE TABLE vendor_ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    
    -- Entry details
    type ledger_entry_type NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    balance_after DECIMAL(15, 2) NOT NULL,
    
    -- Status
    status ledger_entry_status NOT NULL DEFAULT 'COMPLETED',
    
    -- References
    order_id BIGINT REFERENCES orders(id),
    payment_id BIGINT REFERENCES payments(id),
    related_entry_id BIGINT REFERENCES vendor_ledger_entries(id), -- For reversals
    
    -- Description
    description VARCHAR(500),
    reference_number VARCHAR(100),
    
    -- Metadata
    metadata JSONB,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    reversed_at TIMESTAMP WITH TIME ZONE,
    reversed_by BIGINT REFERENCES users(id),
    
    CONSTRAINT vendor_ledger_entries_amount_check CHECK (amount > 0)
);

CREATE INDEX idx_vendor_ledger_vendor_id ON vendor_ledger_entries(vendor_id);
CREATE INDEX idx_vendor_ledger_type ON vendor_ledger_entries(type);
CREATE INDEX idx_vendor_ledger_status ON vendor_ledger_entries(status);
CREATE INDEX idx_vendor_ledger_created_at ON vendor_ledger_entries(created_at DESC);
CREATE INDEX idx_vendor_ledger_order_id ON vendor_ledger_entries(order_id);
CREATE INDEX idx_vendor_ledger_payment_id ON vendor_ledger_entries(payment_id);

-- =====================================================
-- IDEMPOTENCY & DEDUPLICATION
-- =====================================================

-- Idempotency records (prevent duplicate operations)
CREATE TABLE idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    
    -- Scope
    user_id BIGINT REFERENCES users(id),
    vendor_id BIGINT REFERENCES vendors(id),
    
    -- Request details
    endpoint VARCHAR(255) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    request_hash VARCHAR(64) NOT NULL, -- SHA-256 of request body
    
    -- Response
    response_status_code INT,
    response_body TEXT,
    
    -- References
    created_resource_type VARCHAR(100), -- e.g., "ORDER", "PAYMENT"
    created_resource_id BIGINT,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    
    CONSTRAINT idempotency_records_unique_key UNIQUE (idempotency_key, user_id, vendor_id)
);

CREATE INDEX idx_idempotency_key ON idempotency_records(idempotency_key);
CREATE INDEX idx_idempotency_expires_at ON idempotency_records(expires_at);
CREATE INDEX idx_idempotency_user_vendor ON idempotency_records(user_id, vendor_id);

-- =====================================================
-- WEBHOOK & EVENTS
-- =====================================================

-- Webhook events (from eSIM Go and Stripe)
CREATE TABLE webhook_events (
    id BIGSERIAL PRIMARY KEY,
    
    -- Source
    source VARCHAR(50) NOT NULL, -- 'ESIMGO', 'STRIPE'
    event_id VARCHAR(255) NOT NULL, -- External event ID (for deduplication)
    event_type VARCHAR(100) NOT NULL,
    
    -- Payload
    payload JSONB NOT NULL,
    
    -- Processing
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMP WITH TIME ZONE,
    processing_attempts INT DEFAULT 0,
    last_processing_attempt_at TIMESTAMP WITH TIME ZONE,
    
    -- Error tracking
    processing_error TEXT,
    
    -- Related entities
    order_id BIGINT REFERENCES orders(id),
    payment_id BIGINT REFERENCES payments(id),
    
    -- Timestamps
    received_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT webhook_events_unique_event UNIQUE (source, event_id)
);

CREATE INDEX idx_webhook_events_source ON webhook_events(source);
CREATE INDEX idx_webhook_events_event_type ON webhook_events(event_type);
CREATE INDEX idx_webhook_events_processed ON webhook_events(processed);
CREATE INDEX idx_webhook_events_received_at ON webhook_events(received_at DESC);

-- Outgoing webhook deliveries (to vendor webhooks)
CREATE TABLE webhook_deliveries (
    id BIGSERIAL PRIMARY KEY,
    vendor_id BIGINT NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    
    -- Event details
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    
    -- Delivery
    webhook_url VARCHAR(500) NOT NULL,
    http_status_code INT,
    response_body TEXT,
    response_time_ms INT,
    
    -- Retry logic
    attempt_number INT NOT NULL DEFAULT 1,
    max_attempts INT NOT NULL DEFAULT 5,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    
    -- Status
    delivered BOOLEAN DEFAULT FALSE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    failed BOOLEAN DEFAULT FALSE,
    failed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    
    -- Signature (HMAC)
    signature VARCHAR(255),
    
    -- Related entities
    order_id BIGINT REFERENCES orders(id),
    esim_id BIGINT REFERENCES esims(id),
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_webhook_deliveries_vendor_id ON webhook_deliveries(vendor_id);
CREATE INDEX idx_webhook_deliveries_delivered ON webhook_deliveries(delivered);
CREATE INDEX idx_webhook_deliveries_next_retry_at ON webhook_deliveries(next_retry_at) WHERE NOT delivered AND NOT failed;

-- =====================================================
-- API KEYS & ACCESS CONTROL
-- =====================================================

-- API keys (for vendor access and internal services)
CREATE TABLE api_keys (
    id BIGSERIAL PRIMARY KEY,
    
    -- Owner
    vendor_id BIGINT REFERENCES vendors(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id), -- For admin or internal keys
    
    -- Key details
    key_prefix VARCHAR(20) NOT NULL UNIQUE, -- First 8-10 chars for identification
    key_hash VARCHAR(255) NOT NULL UNIQUE, -- bcrypt hash of full key
    name VARCHAR(255) NOT NULL,
    description TEXT,
    
    -- Status and permissions
    status api_key_status NOT NULL DEFAULT 'ACTIVE',
    scopes TEXT[], -- Array of permission scopes
    
    -- Rate limiting
    rate_limit_per_minute INT DEFAULT 60,
    rate_limit_per_hour INT DEFAULT 1000,
    rate_limit_per_day INT DEFAULT 10000,
    
    -- Usage tracking
    last_used_at TIMESTAMP WITH TIME ZONE,
    last_used_ip VARCHAR(45),
    total_requests BIGINT DEFAULT 0,
    
    -- Validity
    expires_at TIMESTAMP WITH TIME ZONE,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_by BIGINT REFERENCES users(id),
    revoked_reason TEXT,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_api_keys_key_prefix ON api_keys(key_prefix);
CREATE INDEX idx_api_keys_key_hash ON api_keys(key_hash);
CREATE INDEX idx_api_keys_vendor_id ON api_keys(vendor_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_api_keys_status ON api_keys(status) WHERE deleted_at IS NULL;

-- API usage tracking
CREATE TABLE api_usage_logs (
    id BIGSERIAL PRIMARY KEY,
    api_key_id BIGINT REFERENCES api_keys(id) ON DELETE CASCADE,
    vendor_id BIGINT REFERENCES vendors(id),
    
    -- Request details
    endpoint VARCHAR(255) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    http_status_code INT,
    
    -- Timing
    response_time_ms INT,
    
    -- Source
    ip_address VARCHAR(45),
    user_agent TEXT,
    
    -- Timestamp (partitioned by day for performance)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_api_usage_logs_api_key_id ON api_usage_logs(api_key_id, created_at DESC);
CREATE INDEX idx_api_usage_logs_vendor_id ON api_usage_logs(vendor_id, created_at DESC);
CREATE INDEX idx_api_usage_logs_created_at ON api_usage_logs(created_at DESC);

-- =====================================================
-- AUDIT LOGGING
-- =====================================================

-- Audit logs (compliance and security tracking)
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    
    -- Actor
    user_id BIGINT REFERENCES users(id),
    vendor_id BIGINT REFERENCES vendors(id),
    actor_type VARCHAR(50) NOT NULL, -- 'USER', 'VENDOR', 'SYSTEM', 'API_KEY'
    actor_identifier VARCHAR(255), -- Email, API key prefix, etc.
    
    -- Action
    action VARCHAR(100) NOT NULL, -- 'CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', etc.
    resource_type VARCHAR(100) NOT NULL, -- 'ORDER', 'PAYMENT', 'ESIM', 'VENDOR', 'USER'
    resource_id BIGINT,
    
    -- Details
    description TEXT,
    changes JSONB, -- Before/after values
    
    -- Context
    ip_address VARCHAR(45),
    user_agent TEXT,
    endpoint VARCHAR(255),
    http_method VARCHAR(10),
    
    -- Result
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    
    -- Timestamp
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_logs_vendor_id ON audit_logs(vendor_id, created_at DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- =====================================================
-- NOTIFICATIONS
-- =====================================================

-- Notifications queue
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    
    -- Recipient
    user_id BIGINT REFERENCES users(id),
    vendor_id BIGINT REFERENCES vendors(id),
    recipient_email VARCHAR(255),
    recipient_phone VARCHAR(50),
    
    -- Notification details
    type notification_type NOT NULL,
    subject VARCHAR(500),
    body TEXT NOT NULL,
    template_code VARCHAR(100),
    template_variables JSONB,
    
    -- Status
    status notification_status NOT NULL DEFAULT 'PENDING',
    
    -- Delivery tracking
    sent_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    attempts INT DEFAULT 0,
    max_attempts INT DEFAULT 3,
    error_message TEXT,
    
    -- External references
    external_id VARCHAR(255), -- Email service provider ID
    
    -- Related entities
    order_id BIGINT REFERENCES orders(id),
    esim_id BIGINT REFERENCES esims(id),
    
    -- Priority
    priority INT DEFAULT 5, -- 1 = highest, 10 = lowest
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    scheduled_for TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_vendor_id ON notifications(vendor_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_scheduled_for ON notifications(scheduled_for) WHERE status = 'PENDING';
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

-- =====================================================
-- SYSTEM CONFIGURATION
-- =====================================================

-- System settings/configuration (key-value store)
CREATE TABLE system_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(255) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    value_type VARCHAR(50) NOT NULL DEFAULT 'STRING', -- 'STRING', 'INTEGER', 'BOOLEAN', 'JSON'
    description TEXT,
    is_sensitive BOOLEAN DEFAULT FALSE, -- Whether to mask in logs
    category VARCHAR(100), -- e.g., 'ESIMGO', 'STRIPE', 'GENERAL', 'LIMITS'
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT REFERENCES users(id)
);

CREATE INDEX idx_system_config_category ON system_config(category);
CREATE INDEX idx_system_config_key ON system_config(config_key);

-- =====================================================
-- INITIAL DATA
-- =====================================================

-- Insert default system configurations
INSERT INTO system_config (config_key, config_value, value_type, description, category) VALUES
('ESIMGO_API_BASE_URL', 'https://api.esim-go.com/v2.4', 'STRING', 'eSIM Go API base URL', 'ESIMGO'),
('ESIMGO_TIMEOUT_SECONDS', '30', 'INTEGER', 'eSIM Go API timeout in seconds', 'ESIMGO'),
('ESIMGO_MAX_RETRIES', '3', 'INTEGER', 'Maximum retry attempts for eSIM Go API calls', 'ESIMGO'),
('STRIPE_API_VERSION', '2025-03-31.basil', 'STRING', 'Stripe API version', 'STRIPE'),
('RATE_LIMIT_USER_PER_MINUTE', '60', 'INTEGER', 'Rate limit for B2C users per minute', 'LIMITS'),
('RATE_LIMIT_VENDOR_PER_MINUTE', '100', 'INTEGER', 'Rate limit for vendors per minute', 'LIMITS'),
('IDEMPOTENCY_TTL_HOURS', '24', 'INTEGER', 'Idempotency record TTL in hours', 'GENERAL'),
('ORDER_RECONCILIATION_CRON', '0 */10 * * * *', 'STRING', 'Cron for order reconciliation (every 10 min)', 'GENERAL'),
('WEBHOOK_RETRY_INTERVALS', '[60, 300, 900, 3600, 14400]', 'JSON', 'Webhook retry intervals in seconds', 'GENERAL'),
('MAX_VENDOR_DAILY_ORDERS', '1000', 'INTEGER', 'Default max daily orders per vendor', 'LIMITS');

-- Create default admin user (password: Admin@123 - MUST BE CHANGED)
INSERT INTO users (email, first_name, last_name, password_hash, role, status, email_verified) VALUES
('admin@ttelgo.com', 'System', 'Administrator', '$2a$10$XQfHvzLFE8X8dPAJ1Q0HhuBJfp6.L6WpYGd.gJ.OJQ8zLX3c6eL5u', 'SUPER_ADMIN', 'ACTIVE', TRUE);

-- =====================================================
-- FUNCTIONS AND TRIGGERS
-- =====================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at trigger to relevant tables
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_vendors_updated_at BEFORE UPDATE ON vendors
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payments_updated_at BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_esims_updated_at BEFORE UPDATE ON esims
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_notifications_updated_at BEFORE UPDATE ON notifications
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_system_config_updated_at BEFORE UPDATE ON system_config
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_webhook_deliveries_updated_at BEFORE UPDATE ON webhook_deliveries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- VIEWS FOR COMMON QUERIES
-- =====================================================

-- View for active orders with customer/vendor info
CREATE VIEW v_orders_with_details AS
SELECT 
    o.id,
    o.order_number,
    o.status,
    o.payment_status,
    o.total_amount,
    o.currency,
    o.bundle_code,
    o.bundle_name,
    o.quantity,
    u.email AS customer_email,
    u.first_name AS customer_first_name,
    u.last_name AS customer_last_name,
    v.name AS vendor_name,
    v.company_name AS vendor_company_name,
    o.created_at,
    o.paid_at,
    o.completed_at,
    CASE 
        WHEN o.vendor_id IS NOT NULL THEN 'B2B'
        ELSE 'B2C'
    END AS order_type
FROM orders o
LEFT JOIN users u ON o.user_id = u.id
LEFT JOIN vendors v ON o.vendor_id = v.id
WHERE o.deleted_at IS NULL;

-- View for vendor financial summary
CREATE VIEW v_vendor_financials AS
SELECT 
    v.id AS vendor_id,
    v.name,
    v.billing_mode,
    v.wallet_balance,
    v.credit_limit,
    v.outstanding_balance,
    v.currency,
    COUNT(DISTINCT o.id) AS total_orders,
    COALESCE(SUM(o.total_amount), 0) AS total_revenue,
    COUNT(DISTINCT CASE WHEN o.status = 'COMPLETED' THEN o.id END) AS completed_orders,
    COUNT(DISTINCT CASE WHEN o.status = 'FAILED' THEN o.id END) AS failed_orders
FROM vendors v
LEFT JOIN orders o ON v.id = o.vendor_id AND o.deleted_at IS NULL
WHERE v.deleted_at IS NULL
GROUP BY v.id, v.name, v.billing_mode, v.wallet_balance, v.credit_limit, v.outstanding_balance, v.currency;

-- =====================================================
-- COMMENTS
-- =====================================================

COMMENT ON TABLE users IS 'Core users table for B2C customers and admin users';
COMMENT ON TABLE vendors IS 'B2B vendor/reseller accounts with billing and limits';
COMMENT ON TABLE orders IS 'All orders (B2C and B2B) for eSIM purchases';
COMMENT ON TABLE payments IS 'Payment records integrated with Stripe PaymentIntent';
COMMENT ON TABLE esims IS 'eSIM inventory with activation codes and status';
COMMENT ON TABLE vendor_ledger_entries IS 'Double-entry ledger for vendor wallet and credit transactions';
COMMENT ON TABLE idempotency_records IS 'Idempotency tracking to prevent duplicate operations';
COMMENT ON TABLE webhook_events IS 'Incoming webhook events from eSIM Go and Stripe';
COMMENT ON TABLE webhook_deliveries IS 'Outgoing webhook deliveries to vendor endpoints';
COMMENT ON TABLE api_keys IS 'API keys for vendor authentication and access control';
COMMENT ON TABLE api_usage_logs IS 'API usage tracking for analytics and rate limiting';
COMMENT ON TABLE audit_logs IS 'Comprehensive audit trail for compliance and security';
COMMENT ON TABLE notifications IS 'Notification queue for emails, SMS, and push notifications';
COMMENT ON TABLE system_config IS 'System-wide configuration key-value store';

-- =====================================================
-- END OF MIGRATION
-- =====================================================
