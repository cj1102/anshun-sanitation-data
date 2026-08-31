CREATE TABLE IF NOT EXISTS t_ad_lease_attachment (
  attachment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  ad_lease_id INT NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  object_name VARCHAR(512) NOT NULL UNIQUE,
  content_type VARCHAR(100) NOT NULL,
  file_size BIGINT NOT NULL,
  uploader_id INT NULL,
  uploader_username VARCHAR(50) NOT NULL DEFAULT 'anonymous',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_lease_attachment_lease FOREIGN KEY (ad_lease_id)
    REFERENCES t_ad_lease_detail(ad_lease_id) ON DELETE CASCADE,
  INDEX idx_lease_attachment_lease_time (ad_lease_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁合同附件元数据，文件实体存储在 MinIO';
