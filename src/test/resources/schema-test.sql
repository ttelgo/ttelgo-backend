-- Test schema initialization for H2 database
-- This creates the audit_logs table that is required by AuditService

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Actor
    user_id BIGINT,
    vendor_id BIGINT,
    actor_type VARCHAR(50) NOT NULL,
    actor_identifier VARCHAR(255),
    
    -- Action
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id BIGINT,
    
    -- Details
    description TEXT,
    changes CLOB, -- JSONB equivalent in H2
    
    -- Context
    ip_address VARCHAR(45),
    user_agent TEXT,
    endpoint VARCHAR(255),
    http_method VARCHAR(10),
    
    -- Result
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    
    -- Timestamp
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
