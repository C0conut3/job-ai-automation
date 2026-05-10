-- 用户信息表（MySQL 8+，utf8mb4）
-- 使用前请确保已创建库：CREATE DATABASE IF NOT EXISTS job_ai ... 并 USE job_ai;

CREATE TABLE IF NOT EXISTS users (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username        VARCHAR(64)  NOT NULL COMMENT '登录名，唯一',
    password_hash   VARCHAR(255) NOT NULL COMMENT 'BCrypt 等算法哈希后的密码',
    email           VARCHAR(128) NULL COMMENT '邮箱，可空，非空时唯一',
    nickname        VARCHAR(64)  NULL COMMENT '昵称',
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
