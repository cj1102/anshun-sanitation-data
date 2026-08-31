INSERT IGNORE INTO sys_permission (permission_code, permission_name, module_name, description)
VALUES ('ai:knowledge:manage', '管理 AI 知识库', 'AI 助手', '上传、查看和删除 AI 知识库文档');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id FROM sys_role r JOIN sys_permission p
WHERE r.role_code='ADMIN' AND p.permission_code='ai:knowledge:manage';

CREATE TABLE IF NOT EXISTS ai_knowledge_document (
  document_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(150) NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  object_name VARCHAR(500) NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  file_size BIGINT NOT NULL,
  visible_roles VARCHAR(255) NOT NULL DEFAULT 'ALL',
  uploader_id INT NOT NULL,
  uploader_username VARCHAR(50) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'READY',
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_knowledge_document_user FOREIGN KEY (uploader_id) REFERENCES t_user(user_id),
  INDEX idx_ai_knowledge_document_visible (deleted, status, create_time DESC)
);

CREATE TABLE IF NOT EXISTS ai_knowledge_chunk (
  chunk_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  document_id BIGINT NOT NULL,
  chunk_no INT NOT NULL,
  page_start INT NULL,
  page_end INT NULL,
  chunk_text MEDIUMTEXT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_knowledge_chunk_document FOREIGN KEY (document_id) REFERENCES ai_knowledge_document(document_id),
  UNIQUE KEY uk_ai_knowledge_chunk_no (document_id, chunk_no),
  INDEX idx_ai_knowledge_chunk_document (document_id, chunk_no)
);
