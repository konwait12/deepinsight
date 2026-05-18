-- ============================================
-- DeepInsight 数据库建表脚本
-- 用途：全新安装时创建users表
-- 数据库：MySQL deepinsight
-- ============================================

CREATE DATABASE IF NOT EXISTS deepinsight
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE deepinsight;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id            INT             NOT NULL AUTO_INCREMENT  COMMENT '用户唯一标识（自增主键）',
    username      VARCHAR(50)     NOT NULL                 COMMENT '用户名（唯一）',
    email         VARCHAR(100)    DEFAULT NULL             COMMENT '用户邮箱（唯一，用于密码找回和通知）',
    role          VARCHAR(20)     NOT NULL DEFAULT 'USER'  COMMENT '用户角色：USER/普通用户 RESEARCHER/研究人员 ADMIN/管理员',
    password_hash VARCHAR(255)    NOT NULL                 COMMENT 'BCrypt password hash',
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账户创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
