-- Fix FAQs active status
-- This script ensures all FAQs are properly marked as active

-- Option 1: Set all FAQs to active (if you want all to show)
UPDATE faqs 
SET is_active = true 
WHERE is_active IS NULL OR is_active = false;

-- Option 2: Check and fix boolean values (in case they're stored incorrectly)
-- If is_active is stored as text 'true'/'false', convert it:
-- UPDATE faqs SET is_active = CASE WHEN is_active::text = 'true' THEN true ELSE false END;

-- Option 3: Verify the fix
SELECT 
    id,
    question,
    is_active,
    display_order,
    category
FROM faqs
WHERE is_active = true
ORDER BY display_order;

-- Count active FAQs
SELECT COUNT(*) as active_faqs_count 
FROM faqs 
WHERE is_active = true;

