-- Diagnostic queries to check FAQs in database

-- 1. Check if table exists and count all FAQs
SELECT 
    COUNT(*) as total_faqs,
    COUNT(*) FILTER (WHERE is_active = true) as active_faqs,
    COUNT(*) FILTER (WHERE is_active = false) as inactive_faqs
FROM faqs;

-- 2. Show all FAQs with their status
SELECT 
    id,
    LEFT(question, 50) as question_preview,
    is_active,
    display_order,
    category,
    created_at
FROM faqs
ORDER BY display_order, id;

-- 3. Check data types and values
SELECT 
    id,
    question,
    pg_typeof(is_active) as is_active_type,
    is_active,
    CASE 
        WHEN is_active = true THEN 'TRUE (boolean)'
        WHEN is_active = false THEN 'FALSE (boolean)'
        WHEN is_active IS NULL THEN 'NULL'
        ELSE 'UNEXPECTED: ' || is_active::text
    END as status_check
FROM faqs
LIMIT 10;

-- 4. Check if there are any FAQs with is_active = true
SELECT 
    id,
    question,
    is_active,
    display_order
FROM faqs
WHERE is_active = true
ORDER BY display_order;

-- 5. If no active FAQs found, show what values exist
SELECT DISTINCT is_active, COUNT(*) 
FROM faqs 
GROUP BY is_active;

