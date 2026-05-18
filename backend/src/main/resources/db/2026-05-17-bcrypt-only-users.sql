-- ============================================
-- DeepInsight migration: enforce BCrypt-only user credentials
-- Safe to run more than once on MySQL 8.
--
-- Legacy MD5+salt passwords cannot be reversed. Accounts that still use
-- non-BCrypt hashes are reset to temporary BCrypt passwords and must be
-- rotated by an administrator before real use.
-- ============================================

USE deepinsight;

SET @db_name = DATABASE();

CREATE TABLE IF NOT EXISTS user_password_migration_audit (
    user_id BIGINT NOT NULL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    previous_hash_prefix VARCHAR(16) NOT NULL,
    reset_reason VARCHAR(120) NOT NULL,
    reset_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO user_password_migration_audit (user_id, username, previous_hash_prefix, reset_reason)
SELECT id, username, LEFT(password_hash, 16), 'non_bcrypt_password_reset'
FROM users
WHERE password_hash NOT LIKE '$2%'
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    previous_hash_prefix = VALUES(previous_hash_prefix),
    reset_reason = VALUES(reset_reason);

-- Temporary password: ChangeMe-Admin-2026!
UPDATE users
SET password_hash = '$2a$10$NrAMHtfQzdqKpnMNzbYxreOslHOLdxYFrzy6VhRIEu7BiWnSnuRCS'
WHERE username = 'admin'
  AND password_hash NOT LIKE '$2%';

-- Temporary password: ChangeMe-Test-2026!
UPDATE users
SET password_hash = '$2a$10$dEURIsLcq21Fsy3hgC5neemx65rB5EmA2cEotizOSsZwQaIgPrToi'
WHERE username = 'testuser'
  AND password_hash NOT LIKE '$2%';

-- Any remaining non-BCrypt accounts get a shared emergency reset password.
-- Temporary password: ChangeMe-User-2026!
UPDATE users
SET password_hash = '$2a$10$hsSWJYljJ5DkdxEJo4ZRmuqPp86kNSMQJrDePqV50EkNRJQcd2QLK'
WHERE password_hash NOT LIKE '$2%';

ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(255) NOT NULL
    COMMENT 'BCrypt password hash';

SET @col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'salt'
);
SET @sql = IF(@col_exists > 0,
    'ALTER TABLE users DROP COLUMN salt',
    'SELECT ''salt column already absent''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
