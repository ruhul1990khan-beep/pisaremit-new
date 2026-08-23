-- ==============================================================================
-- PAISA REMIT (পয়সা রেমিট) - COMPLETE PRODUCTION MYSQL DATABASE SCHEMA (v1.0.0)
-- Engine: InnoDB | Character Set: utf8mb4 | Collation: utf8mb4_unicode_ci
-- ==============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. USERS & CORE AUTHENTICATION
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `user_uuid` VARCHAR(64) NOT NULL UNIQUE,
  `phone` VARCHAR(25) NOT NULL UNIQUE,
  `email` VARCHAR(120) NULL UNIQUE,
  `password_hash` VARCHAR(255) NULL,
  `country_code` VARCHAR(5) DEFAULT 'SAR',
  `nationality` VARCHAR(50) DEFAULT 'Bangladeshi',
  `account_status` ENUM('ACTIVE', 'SUSPENDED', 'BLOCKED', 'KYC_PENDING') DEFAULT 'ACTIVE',
  `kyc_status` ENUM('NOT_SUBMITTED', 'PENDING', 'VERIFIED', 'REJECTED', 'RESUBMISSION_REQUIRED') DEFAULT 'NOT_SUBMITTED',
  `biometric_enabled` TINYINT(1) DEFAULT 1,
  `device_consent_given` TINYINT(1) DEFAULT 1,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL,
  INDEX `idx_users_phone` (`phone`),
  INDEX `idx_users_status` (`account_status`, `kyc_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_profiles` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `full_name_en` VARCHAR(100) NOT NULL,
  `full_name_bn` VARCHAR(100) NULL,
  `date_of_birth` DATE NULL,
  `gender` ENUM('MALE', 'FEMALE', 'OTHER') NULL,
  `present_address` TEXT NULL,
  `permanent_address` TEXT NULL,
  `avatar_url` VARCHAR(255) NULL,
  `occupation` VARCHAR(100) NULL,
  `total_transferred_bdt` DECIMAL(18,2) DEFAULT 0.00,
  `total_transactions_count` INT UNSIGNED DEFAULT 0,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `otp_requests` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `phone` VARCHAR(25) NOT NULL,
  `otp_code` VARCHAR(10) NOT NULL,
  `purpose` ENUM('LOGIN', 'REGISTER', 'TRANSACTION', 'PASSWORD_RESET') DEFAULT 'LOGIN',
  `is_verified` TINYINT(1) DEFAULT 0,
  `attempt_count` TINYINT UNSIGNED DEFAULT 0,
  `expires_at` DATETIME NOT NULL,
  `ip_address` VARCHAR(45) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_otp_phone` (`phone`, `otp_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_sessions` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `session_token` VARCHAR(255) NOT NULL UNIQUE,
  `refresh_token` VARCHAR(255) NULL UNIQUE,
  `device_id` VARCHAR(100) NOT NULL,
  `ip_address` VARCHAR(45) NULL,
  `user_agent` VARCHAR(255) NULL,
  `is_revoked` TINYINT(1) DEFAULT 0,
  `expires_at` DATETIME NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `last_active` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  INDEX `idx_sessions_token` (`session_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. KYC DOCUMENTS
CREATE TABLE IF NOT EXISTS `kyc_profiles` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `document_type` ENUM('PASSPORT', 'NID', 'IQAMA', 'DRIVING_LICENSE') NOT NULL,
  `document_number` VARCHAR(50) NOT NULL,
  `front_image_url` VARCHAR(255) NOT NULL,
  `back_image_url` VARCHAR(255) NULL,
  `selfie_image_url` VARCHAR(255) NOT NULL,
  `verification_status` ENUM('PENDING', 'VERIFIED', 'REJECTED', 'RESUBMISSION_REQUIRED') DEFAULT 'PENDING',
  `admin_reviewer_id` BIGINT UNSIGNED NULL,
  `admin_note` TEXT NULL,
  `submitted_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `reviewed_at` DATETIME NULL,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. BENEFICIARIES (RECIPIENTS)
CREATE TABLE IF NOT EXISTS `beneficiaries` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `beneficiary_uuid` VARCHAR(64) NOT NULL UNIQUE,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `phone` VARCHAR(25) NOT NULL,
  `channel_type` ENUM('BKASH', 'NAGAD', 'ROCKET', 'BANK_ACCOUNT', 'CASH_PICKUP') NOT NULL,
  `bank_or_wallet_name` VARCHAR(100) NOT NULL,
  `account_number` VARCHAR(50) NOT NULL,
  `branch_name` VARCHAR(100) NULL,
  `routing_number` VARCHAR(20) NULL,
  `district` VARCHAR(50) DEFAULT 'Dhaka',
  `relationship` VARCHAR(50) DEFAULT 'Family',
  `is_favorite` TINYINT(1) DEFAULT 1,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  INDEX `idx_ben_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. EXCHANGE RATES & CURRENCIES
CREATE TABLE IF NOT EXISTS `exchange_rates` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `currency_code` VARCHAR(5) NOT NULL UNIQUE,
  `country_name_en` VARCHAR(50) NOT NULL,
  `country_name_bn` VARCHAR(50) NOT NULL,
  `currency_symbol` VARCHAR(10) NOT NULL,
  `flag_emoji` VARCHAR(10) NOT NULL,
  `sending_rate` DECIMAL(12,4) NOT NULL, -- e.g., 32.4500 BDT
  `receiving_rate` DECIMAL(12,4) DEFAULT 1.0000,
  `flat_fee` DECIMAL(10,2) DEFAULT 5.00,
  `percentage_fee` DECIMAL(5,2) DEFAULT 0.00,
  `min_send_amount` DECIMAL(12,2) DEFAULT 50.00,
  `max_send_amount` DECIMAL(12,2) DEFAULT 25000.00,
  `is_active` TINYINT(1) DEFAULT 1,
  `display_order` INT UNSIGNED DEFAULT 1,
  `last_updated` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `exchange_rate_history` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `currency_code` VARCHAR(5) NOT NULL,
  `previous_rate` DECIMAL(12,4) NOT NULL,
  `new_rate` DECIMAL(12,4) NOT NULL,
  `changed_by_admin_id` BIGINT UNSIGNED NULL,
  `notes` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. PAYMENT METHODS
CREATE TABLE IF NOT EXISTS `payment_methods` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `code` VARCHAR(50) NOT NULL UNIQUE,
  `name_en` VARCHAR(100) NOT NULL,
  `name_bn` VARCHAR(100) NOT NULL,
  `icon_type` VARCHAR(30) DEFAULT 'BANK',
  `account_name` VARCHAR(100) NOT NULL,
  `account_number` VARCHAR(100) NOT NULL,
  `instructions_bn` TEXT NOT NULL,
  `instructions_en` TEXT NULL,
  `min_limit` DECIMAL(12,2) DEFAULT 50.00,
  `max_limit` DECIMAL(12,2) DEFAULT 50000.00,
  `requires_proof_upload` TINYINT(1) DEFAULT 1,
  `is_active` TINYINT(1) DEFAULT 1,
  `display_order` INT UNSIGNED DEFAULT 1,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. TRANSACTIONS & REVIEWS
CREATE TABLE IF NOT EXISTS `transactions` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `transaction_ref` VARCHAR(32) NOT NULL UNIQUE, -- e.g., RM-2026-894210
  `user_id` BIGINT UNSIGNED NOT NULL,
  `beneficiary_id` BIGINT UNSIGNED NOT NULL,
  `payment_method_id` INT UNSIGNED NOT NULL,
  `send_amount` DECIMAL(18,2) NOT NULL,
  `send_currency` VARCHAR(5) NOT NULL,
  `exchange_rate` DECIMAL(12,4) NOT NULL,
  `service_fee` DECIMAL(10,2) DEFAULT 0.00,
  `discount_amount` DECIMAL(10,2) DEFAULT 0.00,
  `promo_code_applied` VARCHAR(30) NULL,
  `total_payable` DECIMAL(18,2) NOT NULL,
  `raw_bdt_amount` DECIMAL(18,2) NOT NULL,
  `govt_incentive_bdt` DECIMAL(18,2) DEFAULT 0.00, -- 2.5%
  `recipient_bdt_amount` DECIMAL(18,2) NOT NULL,
  `payment_proof_url` VARCHAR(255) NULL,
  `payment_sender_reference` VARCHAR(100) NULL,
  `status` ENUM('PENDING', 'PROCESSING', 'APPROVED', 'COMPLETED', 'REJECTED', 'REFUND_REQUESTED', 'REFUNDED', 'CANCELLED') DEFAULT 'PENDING',
  `idempotency_key` VARCHAR(64) NOT NULL UNIQUE,
  `admin_note` TEXT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `completed_at` DATETIME NULL,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
  FOREIGN KEY (`beneficiary_id`) REFERENCES `beneficiaries`(`id`),
  FOREIGN KEY (`payment_method_id`) REFERENCES `payment_methods`(`id`),
  INDEX `idx_tx_ref` (`transaction_ref`),
  INDEX `idx_tx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `transaction_status_history` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `transaction_id` BIGINT UNSIGNED NOT NULL,
  `previous_status` VARCHAR(30) NULL,
  `new_status` VARCHAR(30) NOT NULL,
  `title_bn` VARCHAR(100) NOT NULL,
  `description_bn` VARCHAR(255) NOT NULL,
  `changed_by_admin_id` BIGINT UNSIGNED NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`transaction_id`) REFERENCES `transactions`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. OFFERS, NOTICES & NOTIFICATIONS
CREATE TABLE IF NOT EXISTS `offers` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `promo_code` VARCHAR(30) NOT NULL UNIQUE,
  `title_bn` VARCHAR(120) NOT NULL,
  `description_bn` TEXT NOT NULL,
  `discount_percent` DECIMAL(5,2) DEFAULT 0.00,
  `max_discount` DECIMAL(10,2) DEFAULT 0.00,
  `min_send_amount` DECIMAL(12,2) DEFAULT 100.00,
  `badge_text_bn` VARCHAR(50) DEFAULT 'অফার',
  `valid_from` DATE NOT NULL,
  `valid_until` DATE NOT NULL,
  `is_active` TINYINT(1) DEFAULT 1,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `notices` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `title_bn` VARCHAR(150) NOT NULL,
  `message_bn` TEXT NOT NULL,
  `category` ENUM('IMPORTANT', 'TRANSACTION', 'KYC', 'MAINTENANCE', 'OFFER') DEFAULT 'IMPORTANT',
  `is_priority` TINYINT(1) DEFAULT 0,
  `is_active` TINYINT(1) DEFAULT 1,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `notifications` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT UNSIGNED NULL, -- NULL means broadcast to all
  `title_bn` VARCHAR(150) NOT NULL,
  `body_bn` TEXT NOT NULL,
  `type` VARCHAR(30) DEFAULT 'GENERAL',
  `target_url` VARCHAR(255) NULL,
  `is_read` TINYINT(1) DEFAULT 0,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_notif_user` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. DEVICE CONSENT & PROTECTION
CREATE TABLE IF NOT EXISTS `devices` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `device_id` VARCHAR(100) NOT NULL UNIQUE,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `model_name` VARCHAR(100) NOT NULL,
  `os_version` VARCHAR(50) NOT NULL,
  `app_version` VARCHAR(20) NOT NULL,
  `ip_address` VARCHAR(45) NULL,
  `status` ENUM('TRUSTED', 'PENDING', 'BLOCKED', 'REVOKED') DEFAULT 'TRUSTED',
  `consent_timestamp` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `last_active` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  INDEX `idx_device_lookup` (`device_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. SUPPORT TICKETS
CREATE TABLE IF NOT EXISTS `support_tickets` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `ticket_ref` VARCHAR(20) NOT NULL UNIQUE,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `subject` VARCHAR(200) NOT NULL,
  `transaction_ref` VARCHAR(32) NULL,
  `status` ENUM('OPEN', 'REPLIED', 'RESOLVED', 'CLOSED') DEFAULT 'OPEN',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `support_messages` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `ticket_id` BIGINT UNSIGNED NOT NULL,
  `sender_type` ENUM('USER', 'ADMIN') NOT NULL,
  `sender_name` VARCHAR(100) NOT NULL,
  `message` TEXT NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`ticket_id`) REFERENCES `support_tickets`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. ADMIN USERS, ROLES & AUDIT LOGS
CREATE TABLE IF NOT EXISTS `admin_users` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `email` VARCHAR(100) NOT NULL UNIQUE,
  `password_hash` VARCHAR(255) NOT NULL,
  `full_name` VARCHAR(100) NOT NULL,
  `role` ENUM('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'KYC_OFFICER', 'TRANSACTION_OFFICER', 'SUPPORT') DEFAULT 'ADMIN',
  `is_active` TINYINT(1) DEFAULT 1,
  `last_login` DATETIME NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `admin_audit_logs` (
  `id` BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `admin_id` BIGINT UNSIGNED NOT NULL,
  `admin_name` VARCHAR(100) NOT NULL,
  `action` VARCHAR(100) NOT NULL,
  `target_type` VARCHAR(50) NOT NULL,
  `target_id` VARCHAR(64) NOT NULL,
  `notes` TEXT NULL,
  `ip_address` VARCHAR(45) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`admin_id`) REFERENCES `admin_users`(`id`),
  INDEX `idx_audit_action` (`action`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. APP SETTINGS & CONFIGURATION
CREATE TABLE IF NOT EXISTS `app_settings` (
  `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  `setting_key` VARCHAR(64) NOT NULL UNIQUE,
  `setting_value` TEXT NOT NULL,
  `category` VARCHAR(30) DEFAULT 'GENERAL',
  `description` VARCHAR(255) NULL,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
