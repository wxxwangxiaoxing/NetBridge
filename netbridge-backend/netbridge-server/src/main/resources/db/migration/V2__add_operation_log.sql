CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '审计日志ID',
  `user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '操作用户ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
  `action` VARCHAR(50) NOT NULL COMMENT '操作动作',
  `resource_type` VARCHAR(50) NOT NULL COMMENT '资源类型',
  `resource_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '资源ID',
  `result` VARCHAR(20) NOT NULL DEFAULT 'success' COMMENT '结果: success/failed',
  `detail` VARCHAR(500) DEFAULT NULL COMMENT '操作详情',
  `ip_address` VARCHAR(45) DEFAULT NULL COMMENT '来源IP',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_operation_log_user_id` (`user_id`),
  KEY `idx_operation_log_action` (`action`),
  KEY `idx_operation_log_resource` (`resource_type`, `resource_id`),
  KEY `idx_operation_log_result` (`result`),
  KEY `idx_operation_log_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';
