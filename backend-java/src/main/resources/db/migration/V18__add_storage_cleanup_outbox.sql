CREATE TABLE storage_cleanup_task (
  cleanup_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  object_name VARCHAR(512) NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  next_attempt_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_error VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_storage_cleanup_object UNIQUE (object_name),
  CONSTRAINT chk_storage_cleanup_attempts CHECK (attempts >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对象存储删除事务发件箱';

CREATE INDEX idx_storage_cleanup_due ON storage_cleanup_task (next_attempt_at, cleanup_id);
