CREATE TABLE IF NOT EXISTS t_user (
  user_id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL,
  nickname VARCHAR(50), role VARCHAR(20) DEFAULT 'user', status VARCHAR(20) DEFAULT 'active', create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role (
  role_id BIGINT AUTO_INCREMENT PRIMARY KEY, role_code VARCHAR(50) NOT NULL UNIQUE, role_name VARCHAR(50) NOT NULL,
  description VARCHAR(255), status VARCHAR(20) NOT NULL DEFAULT 'active', create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_permission (
  permission_id BIGINT AUTO_INCREMENT PRIMARY KEY, permission_code VARCHAR(100) NOT NULL UNIQUE,
  permission_name VARCHAR(100) NOT NULL, module_name VARCHAR(50) NOT NULL, description VARCHAR(255), create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id INT NOT NULL, role_id BIGINT NOT NULL, create_time DATETIME DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES t_user(user_id) ON DELETE CASCADE,
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(role_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role_permission (
  role_id BIGINT NOT NULL, permission_id BIGINT NOT NULL, create_time DATETIME DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role(role_id) ON DELETE CASCADE,
  CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(permission_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO sys_role (role_code, role_name, description) VALUES
('ADMIN', '系统管理员', '拥有全部系统权限'), ('OPERATOR', '运营专员', '负责广告点位与合同录入'),
('FINANCE', '财务人员', '负责租金与财务统计查看'), ('AUDITOR', '审核员', '负责业务审核与查看'), ('VIEWER', '只读访客', '仅查看获授权的业务数据');

INSERT IGNORE INTO sys_permission (permission_code, permission_name, module_name) VALUES
('position:view','查看广告点位','广告点位'), ('position:create','新增广告点位','广告点位'), ('position:update','编辑广告点位','广告点位'), ('position:delete','删除广告点位','广告点位'),
('lease:view','查看租赁合同','租赁合同'), ('lease:create','录入租赁合同','租赁合同'), ('lease:update','编辑租赁合同','租赁合同'), ('lease:delete','删除租赁合同','租赁合同'),
('stats:view','查看运营统计','运营统计'), ('system:user:manage','管理用户与角色','系统管理');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id) SELECT r.role_id, p.permission_id FROM sys_role r CROSS JOIN sys_permission p WHERE r.role_code='ADMIN';
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) SELECT r.role_id, p.permission_id FROM sys_role r JOIN sys_permission p WHERE r.role_code='OPERATOR' AND p.permission_code IN ('position:view','position:create','position:update','lease:view','lease:create','lease:update','stats:view');
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) SELECT r.role_id, p.permission_id FROM sys_role r JOIN sys_permission p WHERE r.role_code IN ('FINANCE','AUDITOR','VIEWER') AND p.permission_code IN ('position:view','lease:view','stats:view');

INSERT IGNORE INTO t_user (username, password_hash, nickname, role, status) VALUES ('admin', '$2a$10$WgK7gIDO8YTT3lyfzCAqn.Tfq1CYG3ELdaHPpN5UKUEdS9Q8.cC1i', '系统管理员', 'admin', 'active');
INSERT IGNORE INTO sys_user_role (user_id, role_id) SELECT u.user_id, r.role_id FROM t_user u JOIN sys_role r ON r.role_code=CASE WHEN u.role='admin' THEN 'ADMIN' ELSE 'VIEWER' END;
