-- ==============================================================================
-- PAISA REMIT (পয়সা রেমিট) - INITIAL SEED DATA
-- Default Admin: admin / Admin@Paisa2026!
-- ==============================================================================

-- 1. ADMIN USERS (Bcrypt password hash for 'Admin@Paisa2026!')
INSERT INTO `admin_users` (`id`, `username`, `email`, `password_hash`, `full_name`, `role`, `is_active`) VALUES
(1, 'admin', 'admin@paisaremit.com', '$2y$12$0B6M6i/yH53h3g8r.uG6heHl/6aLkJh3b0qX5O0bH3jK9uM3mE3vy', 'Super Administrator', 'SUPER_ADMIN', 1),
(2, 'kycofficer', 'kyc@paisaremit.com', '$2y$12$0B6M6i/yH53h3g8r.uG6heHl/6aLkJh3b0qX5O0bH3jK9uM3mE3vy', 'Senior KYC Officer', 'KYC_OFFICER', 1),
(3, 'txoperator', 'ops@paisaremit.com', '$2y$12$0B6M6i/yH53h3g8r.uG6heHl/6aLkJh3b0qX5O0bH3jK9uM3mE3vy', 'Settlement Officer', 'TRANSACTION_OFFICER', 1);

-- 2. EXCHANGE RATES
INSERT INTO `exchange_rates` (`id`, `currency_code`, `country_name_en`, `country_name_bn`, `currency_symbol`, `flag_emoji`, `sending_rate`, `flat_fee`, `percentage_fee`, `min_send_amount`, `max_send_amount`, `is_active`, `display_order`) VALUES
(1, 'SAR', 'Saudi Arabia', 'সৌদি আরব', '﷼', '🇸🇦', 32.4500, 5.00, 0.00, 50.00, 20000.00, 1, 1),
(2, 'AED', 'United Arab Emirates', 'সংযুক্ত আরব আমিরাত', 'د.إ', '🇦🇪', 33.1000, 5.00, 0.00, 50.00, 20000.00, 1, 2),
(3, 'MYR', 'Malaysia', 'মালয়েশিয়া', 'RM', '🇲🇾', 27.8000, 5.00, 0.00, 50.00, 15000.00, 1, 3),
(4, 'QAR', 'Qatar', 'কাতার', '﷼', '🇶🇦', 33.3500, 5.00, 0.00, 50.00, 20000.00, 1, 4),
(5, 'OMR', 'Oman', 'ওমান', '﷼', '🇴🇲', 315.2000, 0.50, 0.00, 5.00, 2000.00, 1, 5),
(6, 'KWD', 'Kuwait', 'কুয়েত', 'KD', '🇰🇼', 394.5000, 0.50, 0.00, 5.00, 2000.00, 1, 6),
(7, 'BHD', 'Bahrain', 'বাহরাইন', 'BD', '🇧🇭', 321.8000, 0.50, 0.00, 5.00, 2000.00, 1, 7),
(8, 'SGD', 'Singapore', 'সিঙ্গাপুর', '$', '🇸🇬', 91.2000, 5.00, 0.00, 50.00, 10000.00, 1, 8),
(9, 'GBP', 'United Kingdom', 'যুক্তরাজ্য (UK)', '£', '🇬🇧', 154.6000, 2.00, 0.00, 20.00, 10000.00, 1, 9),
(10, 'USD', 'United States', 'যুক্তরাষ্ট্র (USA)', '$', '🇺🇸', 121.5000, 3.00, 0.00, 20.00, 10000.00, 1, 10);

-- 3. PAYMENT METHODS
INSERT INTO `payment_methods` (`id`, `code`, `name_en`, `name_bn`, `icon_type`, `account_name`, `account_number`, `instructions_bn`, `requires_proof_upload`, `is_active`, `display_order`) VALUES
(1, 'ALRAJHI_BANK', 'Al Rajhi Bank Transfer', 'আল রাজি ব্যাংক ট্রান্সফার', 'BANK', 'Paisa Remit Global Ltd', 'SA448000020160801009988', 'আপনার আল রাজি অ্যাপ থেকে নির্দিষ্ট IBAN-এ টাকা পাঠিয়ে রেফারেন্স নম্বর ও স্ক্রিনশট আপলোড করুন।', 1, 1, 1),
(2, 'STC_PAY', 'stc pay / Wio Direct', 'এসটিসি পে (stc pay)', 'WALLET', 'Paisa Remit KSA', '+966 50 123 4567', 'এসটিসি পে মার্চেন্ট বা কিউআর কোডে সরাসরি সেন্ড মানি করুন। কোনো বাড়তি চার্জ নেই।', 1, 1, 2),
(3, 'SNB_ALAHLAI', 'SNB AlAhli Online Banking', 'এসএনবি আল আহলি ব্যাংক', 'BANK', 'Paisa Remit Int Services', 'SA12100000030998877665', 'লোকাল এসএনবি ব্যাংক ট্রান্সফার করুন এবং রেফারেন্স নোট হিসেবে আপনার ইউজার আইডি প্রদান করুন।', 1, 1, 3);

-- 4. OFFERS
INSERT INTO `offers` (`id`, `promo_code`, `title_bn`, `description_bn`, `discount_percent`, `max_discount`, `min_send_amount`, `badge_text_bn`, `valid_from`, `valid_until`, `is_active`) VALUES
(1, 'PAISANEW', 'প্রথম রেমিট্যান্সে ১০০% ফি মওকুফ', 'নতুন গ্রাহকদের জন্য প্রথম রেমিট্যান্সে সার্ভিস ফি সম্পূর্ণ ফ্রী! কোড: PAISANEW', 100.00, 20.00, 100.00, 'নতুন গ্রাহক অফার', '2026-01-01', '2026-12-31', 1),
(2, 'PROBASHI50', '৫০% সার্ভিস ফি ডিসকাউন্ট', 'যেকোনো দেশে ৫০০+ রিয়াল বা সমপরিমাণ পাঠালে ৫০% ফি ডিসকাউন্ট পান।', 50.00, 10.00, 500.00, 'স্পেশাল ছাড়', '2026-01-01', '2026-12-31', 1),
(3, 'GOVT25', 'সরকারি ২.৫% সরাসরি নগদ প্রণোদনা', 'বৈধ ব্যাংকিং চ্যানেলে রেমিট্যান্স পাঠিয়ে সরকারের পক্ষ থেকে অতিরিক্ত ২.৫% ক্যাশব্যাক সরাসরি অ্যাকাউন্টে বুঝে নিন।', 0.00, 0.00, 50.00, 'সরকারি প্রণোদনা', '2026-01-01', '2026-12-31', 1);

-- 5. NOTICES
INSERT INTO `notices` (`id`, `title_bn`, `message_bn`, `category`, `is_priority`, `is_active`) VALUES
(1, 'পয়সা রেমিট প্ল্যাটফর্মে আপনাকে স্বাগতম!', 'প্রবাসীদের ঘামের প্রতিটি পয়সা নিরাপদে দেশে পরিবারের কাছে পৌঁছে দিতে আমরা প্রতিশ্রুতিবদ্ধ।', 'IMPORTANT', 1, 1),
(2, 'বাংলাদেশ ব্যাংক অনুমোদিত ২.৫% সরকারি প্রণোদনা চালু রয়েছে', 'সকল রেমিট্যান্সের মূল টাকার সাথে অতিরিক্ত ২.৫% প্রণোদনা সরাসরি প্রাপকের ব্যাংক বা বিকাশ অ্যাকাউন্টে যোগ হবে।', 'TRANSACTION', 0, 1),
(3, 'সাপ্তাহিক ও সরকারি ছুটির দিনেও বিকাশ ও নগদ ইনস্ট্যান্ট সেটেলমেন্ট চালু', 'ছুটির দিনেও বিকাশ, নগদ এবং রকেট ওয়ালেটে কয়েক মিনিটের মধ্যে টাকা ট্রান্সফার সম্পন্ন হয়।', 'IMPORTANT', 0, 1);

-- 6. APP SETTINGS
INSERT INTO `app_settings` (`setting_key`, `setting_value`, `category`, `description`) VALUES
('app_name', 'Paisa Remit', 'GENERAL', 'Application Name'),
('app_name_bn', 'পয়সা রেমিট', 'GENERAL', 'Application Name in Bengali'),
('company_name', 'Paisa Remit Global Technologies Ltd.', 'GENERAL', 'Registered Company Name'),
('helpline_phone', '+880 9612-888999', 'SUPPORT', 'Customer Helpline Number'),
('whatsapp_number', '+966 50 123 4567', 'SUPPORT', 'WhatsApp Support Number'),
('support_email', 'support@paisaremit.com', 'SUPPORT', 'Official Support Email Address'),
('office_hours_bn', 'সকাল ৯:০০ - রাত ১১:০০ (সৌদি সময়)', 'SUPPORT', 'Support Working Hours'),
('govt_incentive_percent', '2.5', 'REMITTANCE', 'Bangladesh Govt Cash Incentive Percentage'),
('min_remittance_amount_sar', '50.00', 'REMITTANCE', 'Minimum Remittance Amount in SAR'),
('max_daily_remittance_limit', '25000.00', 'REMITTANCE', 'Maximum Daily Remittance Limit'),
('maintenance_mode', '0', 'SYSTEM', 'System Maintenance Mode (0=Live, 1=Maintenance)'),
('maintenance_message_bn', 'আমাদের সার্ভার আপগ্রেড চলছে। কিছুক্ষণের মধ্যেই সেবা পুনরায় চালু হবে।', 'SYSTEM', 'Maintenance Display Message');
