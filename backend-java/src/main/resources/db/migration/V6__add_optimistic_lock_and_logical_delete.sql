ALTER TABLE t_ad_position
  ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记';

ALTER TABLE t_ad_lease_detail
  ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记';

CREATE INDEX idx_position_deleted_status_district ON t_ad_position (deleted, status, district);
CREATE INDEX idx_lease_deleted_start_date ON t_ad_lease_detail (deleted, lease_start_date);
