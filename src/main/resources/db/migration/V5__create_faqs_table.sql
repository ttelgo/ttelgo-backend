-- Create faqs table
CREATE TABLE IF NOT EXISTS faqs (
    id BIGSERIAL PRIMARY KEY,
    question VARCHAR(500) NOT NULL,
    answer TEXT NOT NULL,
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    category VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index on is_active for filtering
CREATE INDEX IF NOT EXISTS idx_faqs_active ON faqs(is_active) WHERE is_active = TRUE;

-- Create index on category for filtering
CREATE INDEX IF NOT EXISTS idx_faqs_category ON faqs(category);

-- Create index on display_order for sorting
CREATE INDEX IF NOT EXISTS idx_faqs_display_order ON faqs(display_order);

