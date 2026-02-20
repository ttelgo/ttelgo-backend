-- Create audit_logs table manually (since Flyway is disabled in dev)
-- Run this script in your PostgreSQL database: psql -U postgres -d ttelgo_dev -f create_audit_logs_table.sql

CREATE TABLE IF NOT EXISTS audit_logs (
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

CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_vendor_id ON audit_logs(vendor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at DESC);
