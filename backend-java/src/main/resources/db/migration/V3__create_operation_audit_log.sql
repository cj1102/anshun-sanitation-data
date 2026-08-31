CREATE TABLE IF NOT EXISTS sys_operation_log (
  log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  operator_id INT NULL,
  operator_username VARCHAR(50) NOT NULL DEFAULT 'anonymous',
  module_name VARCHAR(50) NOT NULL,
  action_name VARCHAR(100) NOT NULL,
  target_id VARCHAR(100) NULL,
  request_method VARCHAR(10) NULL,
  request_path VARCHAR(255) NULL,
  request_id VARCHAR(64) NULL,
  client_ip VARCHAR(64) NULL,
  success TINYINT(1) NOT NULL,
  error_message VARCHAR(500) NULL,
  duration_ms BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_audit_operator_time (operator_id, create_time),
  INDEX idx_audit_module_time (module_name, create_time),
  INDEX idx_audit_request_id (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关键业务操作审计日志';

INSERT IGNORE INTO sys_permission (permission_code, permission_name, module_name)
VALUES ('system:audit:view', '查看操作审计日志', '系统管理');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id FROM sys_role r JOIN sys_permission p
WHERE r.role_code='ADMIN' AND p.permission_code='system:audit:view';
