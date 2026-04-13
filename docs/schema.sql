-- 文件服务数据库脚本

-- DROP DATABASE IF EXISTS file_server;
-- CREATE DATABASE file_server DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE file_server;

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '加密密码',
  `email` varchar(128) COMMENT '邮箱',
  `role` varchar(32) NOT NULL DEFAULT 'USER' COMMENT '角色: ADMIN/USER',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_login` datetime COMMENT '最后登录时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 文件信息表
CREATE TABLE IF NOT EXISTS `file_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `file_hash` varchar(128) NOT NULL COMMENT '文件SHA-256哈希值',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_size` bigint NOT NULL COMMENT '文件大小(字节)',
  `file_path` varchar(512) NOT NULL COMMENT '本地存储路径',
  `mime_type` varchar(128) COMMENT 'MIME类型',
  `upload_user_id` bigint NOT NULL COMMENT '上传用户ID',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
  `delete_time` datetime COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_hash` (`file_hash`),
  KEY `idx_upload_user_id` (`upload_user_id`),
  KEY `idx_upload_time` (`upload_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件信息表';

-- 文件分享表
CREATE TABLE IF NOT EXISTS `file_share` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分享ID',
  `file_id` bigint NOT NULL COMMENT '文件ID',
  `share_token` varchar(64) NOT NULL COMMENT '分享令牌(UUID)',
  `share_user_id` bigint NOT NULL COMMENT '分享发起者ID',
  `expire_time` datetime COMMENT '过期时间(NULL表示不过期)',
  `max_downloads` int COMMENT '最大下载次数(NULL表示无限)',
  `current_downloads` int NOT NULL DEFAULT 0 COMMENT '当前下载次数',
  `is_active` tinyint NOT NULL DEFAULT 1 COMMENT '是否活跃: 1-活跃, 0-已失效',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_share_token` (`share_token`),
  KEY `idx_file_id` (`file_id`),
  KEY `idx_share_user_id` (`share_user_id`),
  KEY `idx_expire_time` (`expire_time`),
  KEY `idx_is_active` (`is_active`),
  CONSTRAINT `fk_file_id` FOREIGN KEY (`file_id`) REFERENCES `file_info` (`id`),
  CONSTRAINT `fk_share_user_id` FOREIGN KEY (`share_user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件分享表';

-- 访问日志表
CREATE TABLE IF NOT EXISTS `sys_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint COMMENT '用户ID',
  `operation` varchar(128) NOT NULL COMMENT '操作类型: UPLOAD/DOWNLOAD/SHARE/DELETE',
  `resource_type` varchar(64) COMMENT '资源类型',
  `resource_id` bigint COMMENT '资源ID',
  `status` varchar(32) NOT NULL COMMENT '操作状态: SUCCESS/FAIL',
  `error_msg` text COMMENT '错误信息',
  `client_ip` varchar(64) COMMENT '客户端IP',
  `user_agent` varchar(512) COMMENT '请求代理',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operation` (`operation`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统日志表';

-- 限流配置表
CREATE TABLE IF NOT EXISTS `rate_limit` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '限流ID',
  `share_token` varchar(64) NOT NULL COMMENT '分享令牌',
  `request_count` int NOT NULL DEFAULT 0 COMMENT '请求次数',
  `reset_time` datetime NOT NULL COMMENT '重置时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_share_token_reset` (`share_token`, `reset_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='限流配置表';

-- 初始化管理员账户
INSERT INTO `sys_user` (`username`, `password`, `email`, `role`) 
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcg7b3XeKeUxWdeS86E36gBUono', 'admin@example.com', 'ADMIN')
ON DUPLICATE KEY UPDATE `id` = `id`;

-- 创建索引以优化查询
CREATE INDEX idx_file_info_hash_time ON file_info(file_hash, upload_time);
CREATE INDEX idx_file_share_token_active ON file_share(share_token, is_active);
CREATE INDEX idx_sys_log_user_time ON sys_log(user_id, create_time);
