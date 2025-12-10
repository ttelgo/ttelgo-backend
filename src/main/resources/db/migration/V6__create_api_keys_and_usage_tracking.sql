-- Create api_keys table
CREATE TABLE IF NOT EXISTS api_keys (
    id BIGSERIAL PRIMARY KEY,
    key_name VARCHAR(255) NOT NULL,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    api_secret VARCHAR(255) NOT NULL,
    user_id BIGINT,
    customer_name VARCHAR(255),
    customer_email VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    rate_limit_per_minute INTEGER DEFAULT 60,
    rate_limit_per_hour INTEGER DEFAULT 1000,
    rate_limit_per_day INTEGER DEFAULT 10000,
    allowed_ips TEXT, -- JSON array of allowed IPs
    scopes TEXT, -- JSON array of allowed scopes/endpoints
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    notes TEXT
);

-- Create index on api_key for fast lookups
CREATE INDEX IF NOT EXISTS idx_api_keys_key ON api_keys(api_key);
CREATE INDEX IF NOT EXISTS idx_api_keys_user_id ON api_keys(user_id);
CREATE INDEX IF NOT EXISTS idx_api_keys_active ON api_keys(is_active) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_api_keys_customer_email ON api_keys(customer_email);

-- Create api_usage_logs table for tracking API usage
CREATE TABLE IF NOT EXISTS api_usage_logs (
    id BIGSERIAL PRIMARY KEY,
    api_key_id BIGINT NOT NULL REFERENCES api_keys(id) ON DELETE CASCADE,
    endpoint VARCHAR(500) NOT NULL,
    method VARCHAR(10) NOT NULL,
    status_code INTEGER,
    response_time_ms INTEGER,
    request_size_bytes INTEGER,
    response_size_bytes INTEGER,
    ip_address VARCHAR(45),
    user_agent TEXT,
    request_body TEXT,
    response_body TEXT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for API usage logs
CREATE INDEX IF NOT EXISTS idx_api_usage_logs_api_key_id ON api_usage_logs(api_key_id);
CREATE INDEX IF NOT EXISTS idx_api_usage_logs_created_at ON api_usage_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_api_usage_logs_endpoint ON api_usage_logs(endpoint);
CREATE INDEX IF NOT EXISTS idx_api_usage_logs_status_code ON api_usage_logs(status_code);

-- Create api_rate_limit_tracking table for rate limit enforcement
CREATE TABLE IF NOT EXISTS api_rate_limit_tracking (
    id BIGSERIAL PRIMARY KEY,
    api_key_id BIGINT NOT NULL REFERENCES api_keys(id) ON DELETE CASCADE,
    window_type VARCHAR(20) NOT NULL, -- 'minute', 'hour', 'day'
    window_start TIMESTAMP NOT NULL,
    request_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(api_key_id, window_type, window_start)
);

-- Create index for rate limit tracking
CREATE INDEX IF NOT EXISTS idx_rate_limit_api_key_window ON api_rate_limit_tracking(api_key_id, window_type, window_start);

