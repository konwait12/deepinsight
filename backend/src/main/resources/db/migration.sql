-- ============================================
-- DeepInsight 数据库迁移脚本
-- 用途：将旧版users表结构与JPA实体对齐
-- 执行环境：MySQL deepinsight数据库（v5.7+）
-- ============================================

USE deepinsight;

-- 1. 添加email列（如果不存在）
--    用户邮箱，用于密码找回和平台通知
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'deepinsight' AND TABLE_NAME = 'users' AND COLUMN_NAME = 'email');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN email VARCHAR(100) UNIQUE COMMENT ''用户邮箱地址'' AFTER username',
    'SELECT ''email列已存在''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 添加role列（如果不存在）
--    用户角色，默认USER
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'deepinsight' AND TABLE_NAME = 'users' AND COLUMN_NAME = 'role');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT ''USER'' COMMENT ''用户角色'' AFTER email',
    'SELECT ''role列已存在''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. Ensure password_hash can store BCrypt hashes.
ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(255) NOT NULL
    COMMENT 'BCrypt password hash';

-- 4. Remove obsolete standalone salt column.
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'deepinsight' AND TABLE_NAME = 'users' AND COLUMN_NAME = 'salt');
SET @sql = IF(@col_exists > 0,
    'ALTER TABLE users DROP COLUMN salt',
    'SELECT ''salt column already absent''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
