-- Verification query to check if all FAQs indexes exist
-- Run this to verify all indexes were created successfully

-- Check table structure
SELECT 
    column_name, 
    data_type, 
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'faqs' 
ORDER BY ordinal_position;

-- Check all indexes on faqs table
SELECT 
    indexname,
    indexdef
FROM pg_indexes 
WHERE tablename = 'faqs'
ORDER BY indexname;

-- Expected indexes:
-- 1. faqs_pkey (primary key on id) - automatically created
-- 2. idx_faqs_active - partial index on is_active WHERE is_active = TRUE
-- 3. idx_faqs_category - index on category
-- 4. idx_faqs_display_order - index on display_order

