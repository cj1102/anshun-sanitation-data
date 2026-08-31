-- Existing imported contracts are historical effective contracts, so they remain APPROVED.
-- New contracts start as DRAFT and only become effective after an AUDITOR/ADMIN approval.
ALTER TABLE t_ad_lease_detail
  ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED' COMMENT 'DRAFT/PENDING/APPROVED/REJECTED',
  ADD COLUMN submitted_by_username VARCHAR(50) NULL COMMENT '提交审核的用户',
  ADD COLUMN submitted_at DATETIME NULL COMMENT '提交审核时间',
  ADD COLUMN approver_username VARCHAR(50) NULL COMMENT '审核人',
  ADD COLUMN approved_at DATETIME NULL COMMENT '审核完成时间',
  ADD COLUMN approval_comment VARCHAR(500) NULL COMMENT '审核意见';

CREATE INDEX idx_lease_approval_position_period
  ON t_ad_lease_detail (approval_status, deleted, ad_position_code, lease_start_date, lease_end_date);

INSERT IGNORE INTO sys_permission (permission_code, permission_name, module_name, description) VALUES
('lease:submit', '提交合同审核', '租赁合同', '将合同草稿提交给审核员'),
('lease:approve', '审核租赁合同', '租赁合同', '通过或驳回待审核合同');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id FROM sys_role r JOIN sys_permission p
WHERE r.role_code='ADMIN' AND p.permission_code IN ('lease:submit', 'lease:approve');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id FROM sys_role r JOIN sys_permission p
WHERE r.role_code='OPERATOR' AND p.permission_code='lease:submit';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id FROM sys_role r JOIN sys_permission p
WHERE r.role_code='AUDITOR' AND p.permission_code='lease:approve';
