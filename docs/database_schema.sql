-- NetBridge database schema
-- Target: MySQL 8.0+
-- Note: This version intentionally does not use foreign key constraints.

CREATE DATABASE IF NOT EXISTS `netbridge`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `netbridge`;

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码哈希',
  `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
  `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色: user/admin',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
  `last_login_at` DATETIME NULL DEFAULT NULL COMMENT '最后登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_email` (`email`),
  KEY `idx_user_role` (`role`),
  KEY `idx_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `server` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '服务器ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
  `agent_id` VARCHAR(64) DEFAULT NULL COMMENT 'Agent唯一标识',
  `hostname` VARCHAR(100) NOT NULL COMMENT '主机名',
  `display_name` VARCHAR(100) DEFAULT NULL COMMENT '展示名称',
  `os` VARCHAR(50) NOT NULL COMMENT '操作系统',
  `arch` VARCHAR(20) DEFAULT NULL COMMENT 'CPU架构',
  `ip` VARCHAR(45) NOT NULL COMMENT '公网IP/主IP',
  `tailscale_ip` VARCHAR(45) DEFAULT NULL COMMENT 'Tailscale IP',
  `status` VARCHAR(20) NOT NULL DEFAULT 'offline' COMMENT '状态: online/offline/error',
  `agent_version` VARCHAR(20) DEFAULT NULL COMMENT 'Agent版本',
  `cpu` VARCHAR(100) DEFAULT NULL COMMENT 'CPU信息',
  `memory` VARCHAR(100) DEFAULT NULL COMMENT '内存信息',
  `disk` VARCHAR(100) DEFAULT NULL COMMENT '磁盘信息',
  `last_heartbeat` DATETIME DEFAULT NULL COMMENT '最后心跳时间',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_server_agent_id` (`agent_id`),
  KEY `idx_server_user_id` (`user_id`),
  KEY `idx_server_status` (`status`),
  KEY `idx_server_last_heartbeat` (`last_heartbeat`),
  KEY `idx_server_hostname` (`hostname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务器表';

CREATE TABLE IF NOT EXISTS `service` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '服务ID',
  `server_id` BIGINT UNSIGNED NOT NULL COMMENT '所属服务器ID',
  `name` VARCHAR(100) NOT NULL COMMENT '服务名称',
  `type` VARCHAR(30) NOT NULL COMMENT '服务类型: MySQL/Redis/Nacos/Web/SSH等',
  `protocol` VARCHAR(10) NOT NULL COMMENT '协议: TCP/HTTP/HTTPS',
  `port` INT NOT NULL COMMENT '服务端口',
  `target_host` VARCHAR(255) DEFAULT '127.0.0.1' COMMENT '目标服务地址',
  `domain` VARCHAR(255) DEFAULT NULL COMMENT '绑定域名',
  `subdomain` VARCHAR(100) DEFAULT NULL COMMENT '自动分配的子域名前缀',
  `enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用: 1启用 0禁用',
  `status` VARCHAR(20) NOT NULL DEFAULT 'inactive' COMMENT '状态: active/inactive/error',
  `access_type` VARCHAR(20) NOT NULL COMMENT '访问类型: tailscale/tunnel',
  `access_url` VARCHAR(500) DEFAULT NULL COMMENT '生成的访问地址或命令',
  `health_status` VARCHAR(20) NOT NULL DEFAULT 'unknown' COMMENT '健康状态: healthy/warning/error/unknown',
  `last_check_at` DATETIME DEFAULT NULL COMMENT '最后检测时间',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_service_server_id` (`server_id`),
  KEY `idx_service_type` (`type`),
  KEY `idx_service_status` (`status`),
  KEY `idx_service_enabled` (`enabled`),
  KEY `idx_service_access_type` (`access_type`),
  KEY `idx_service_domain` (`domain`),
  UNIQUE KEY `uk_service_server_port_name` (`server_id`, `port`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务表';

CREATE TABLE IF NOT EXISTS `task` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `server_id` BIGINT UNSIGNED NOT NULL COMMENT '所属服务器ID',
  `service_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联服务ID',
  `agent_id` VARCHAR(64) DEFAULT NULL COMMENT '执行任务的Agent ID',
  `type` VARCHAR(30) NOT NULL COMMENT '任务类型: INSTALL/START/STOP/RESTART/DELETE/CHECK',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/running/success/failed/cancelled',
  `payload` JSON DEFAULT NULL COMMENT '任务参数',
  `result` JSON DEFAULT NULL COMMENT '任务结果',
  `error_message` TEXT COMMENT '错误信息',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  `started_at` DATETIME DEFAULT NULL COMMENT '开始时间',
  `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_server_id` (`server_id`),
  KEY `idx_task_service_id` (`service_id`),
  KEY `idx_task_agent_id` (`agent_id`),
  KEY `idx_task_type` (`type`),
  KEY `idx_task_status` (`status`),
  KEY `idx_task_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

CREATE TABLE IF NOT EXISTS `log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `server_id` BIGINT UNSIGNED NOT NULL COMMENT '服务器ID',
  `service_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '服务ID',
  `task_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '任务ID',
  `agent_id` VARCHAR(64) DEFAULT NULL COMMENT 'Agent ID',
  `level` VARCHAR(10) NOT NULL COMMENT '日志级别: INFO/WARN/ERROR/DEBUG',
  `source` VARCHAR(50) NOT NULL COMMENT '日志来源: agent/tunnel/system',
  `content` TEXT NOT NULL COMMENT '日志内容',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_log_server_id` (`server_id`),
  KEY `idx_log_service_id` (`service_id`),
  KEY `idx_log_task_id` (`task_id`),
  KEY `idx_log_level` (`level`),
  KEY `idx_log_source` (`source`),
  KEY `idx_log_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运行日志表';

CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '操作记录ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '操作用户ID',
  `action` VARCHAR(50) NOT NULL COMMENT '操作类型',
  `target_type` VARCHAR(20) NOT NULL COMMENT '目标类型: server/service/task/config',
  `target_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '目标ID',
  `details` JSON DEFAULT NULL COMMENT '操作详情',
  `ip` VARCHAR(45) NOT NULL COMMENT '操作IP',
  `user_agent` VARCHAR(255) DEFAULT NULL COMMENT '请求UA',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_operation_log_user_id` (`user_id`),
  KEY `idx_operation_log_action` (`action`),
  KEY `idx_operation_log_target` (`target_type`, `target_id`),
  KEY `idx_operation_log_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';

CREATE TABLE IF NOT EXISTS `config` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` TEXT NOT NULL COMMENT '配置值',
  `value_type` VARCHAR(20) NOT NULL DEFAULT 'string' COMMENT '值类型: string/number/bool/json',
  `is_encrypted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否加密存储: 1是 0否',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '配置描述',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

CREATE TABLE IF NOT EXISTS `server_metric` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '监控ID',
  `server_id` BIGINT UNSIGNED NOT NULL COMMENT '服务器ID',
  `cpu_usage` DECIMAL(5,2) DEFAULT NULL COMMENT 'CPU使用率',
  `memory_usage` DECIMAL(5,2) DEFAULT NULL COMMENT '内存使用率',
  `disk_usage` DECIMAL(5,2) DEFAULT NULL COMMENT '磁盘使用率',
  `network_in_bytes` BIGINT UNSIGNED DEFAULT NULL COMMENT '入站流量',
  `network_out_bytes` BIGINT UNSIGNED DEFAULT NULL COMMENT '出站流量',
  `load_average` DECIMAL(6,2) DEFAULT NULL COMMENT '平均负载',
  `collected_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间',
  PRIMARY KEY (`id`),
  KEY `idx_server_metric_server_time` (`server_id`, `collected_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务器监控指标表';

CREATE TABLE IF NOT EXISTS `service_metric` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '监控ID',
  `service_id` BIGINT UNSIGNED NOT NULL COMMENT '服务ID',
  `server_id` BIGINT UNSIGNED NOT NULL COMMENT '服务器ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0异常',
  `response_time_ms` INT DEFAULT NULL COMMENT '响应时间毫秒',
  `success_count` BIGINT UNSIGNED DEFAULT NULL COMMENT '成功次数',
  `error_count` BIGINT UNSIGNED DEFAULT NULL COMMENT '错误次数',
  `collected_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间',
  PRIMARY KEY (`id`),
  KEY `idx_service_metric_service_time` (`service_id`, `collected_at`),
  KEY `idx_service_metric_server_time` (`server_id`, `collected_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务监控指标表';

CREATE TABLE IF NOT EXISTS `install_token` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '安装令牌ID',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '创建用户ID',
  `token` VARCHAR(128) NOT NULL COMMENT '安装令牌',
  `hostname` VARCHAR(100) DEFAULT NULL COMMENT '预期主机名',
  `status` VARCHAR(20) NOT NULL DEFAULT 'unused' COMMENT '状态: unused/used/expired/revoked',
  `expires_at` DATETIME NOT NULL COMMENT '过期时间',
  `used_at` DATETIME DEFAULT NULL COMMENT '使用时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_install_token_token` (`token`),
  KEY `idx_install_token_user_id` (`user_id`),
  KEY `idx_install_token_status` (`status`),
  KEY `idx_install_token_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务器安装令牌表';
