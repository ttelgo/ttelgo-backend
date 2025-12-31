-- Insert Sample FAQs into the database
-- These are the FAQs from the frontend faqsData.ts file

INSERT INTO faqs (question, answer, display_order, is_active, category, created_at, updated_at) VALUES
('What is eSIM?', 'An eSIM (embedded SIM) is a digital SIM card that allows you to activate a cellular plan without a physical SIM card.', 1, true, 'General', NOW(), NOW()),
('Is my device compatible with TTelGo eSIM?', 'Most modern smartphones support eSIM, including iPhone XS and newer, Google Pixel 3 and newer, and Samsung Galaxy S20 and newer.', 2, true, 'Device Compatibility', NOW(), NOW()),
('How to install eSIM?', 'Go to your device settings, select "Add Cellular Plan", scan the QR code we provide, and follow the on-screen instructions.', 3, true, 'Setup & Installation', NOW(), NOW()),
('What if I run out of data?', 'You can purchase additional data packs or upgrade to a higher plan through your account dashboard.', 4, true, 'Data & Plans', NOW(), NOW()),
('Is hotspot and tethering supported?', 'Yes, most of our plans support hotspot and tethering. Check your plan details to confirm.', 5, true, 'Features', NOW(), NOW()),
('Does the eSIM include a Local Phone Number?', 'No, our eSIMs are data-only and do not include a phone number for calls or SMS.', 6, true, 'Features', NOW(), NOW()),
('How much data do I need while travelling?', 'This depends on your usage. Light users may need 1-2GB per week, while heavy users may need 5GB or more.', 7, true, 'Data & Plans', NOW(), NOW()),
('How does using an eSIM compare to Pocket WiFi?', 'eSIM is more convenient as it doesn''t require carrying an extra device, and it works directly in your phone.', 8, true, 'General', NOW(), NOW()),
('Are there long-term plans available?', 'Yes, we offer plans with validity periods ranging from 7 days to 30 days, with options to extend or add more data.', 9, true, 'Data & Plans', NOW(), NOW()),
('Can I keep my primary SIM while using TTelGo eSIM?', 'Yes, most devices support dual SIM functionality, allowing you to use both your primary SIM and TTelGo eSIM simultaneously.', 10, true, 'Device Compatibility', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Verify the FAQs were inserted
SELECT id, question, category, is_active, display_order FROM faqs ORDER BY display_order;

