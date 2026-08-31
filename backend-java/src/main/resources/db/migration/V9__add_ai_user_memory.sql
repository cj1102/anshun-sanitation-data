CREATE TABLE IF NOT EXISTS ai_user_memory (
  memory_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  memory_type VARCHAR(32) NOT NULL DEFAULT 'OTHER',
  content VARCHAR(500) NOT NULL,
  content_hash CHAR(64) NOT NULL,
  source VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_user_memory_user FOREIGN KEY (user_id) REFERENCES t_user(user_id),
  UNIQUE KEY uk_ai_user_memory_content (user_id, content_hash),
  INDEX idx_ai_user_memory_user_time (user_id, create_time DESC)
);
