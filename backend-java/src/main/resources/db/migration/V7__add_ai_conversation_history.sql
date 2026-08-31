CREATE TABLE IF NOT EXISTS ai_conversation (
  conversation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  title VARCHAR(100) NOT NULL,
  last_message_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_conversation_user FOREIGN KEY (user_id) REFERENCES t_user(user_id),
  INDEX idx_ai_conversation_user_recent (user_id, deleted, last_message_at DESC)
);

CREATE TABLE IF NOT EXISTS ai_chat_message (
  message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  conversation_id BIGINT NOT NULL,
  role ENUM('user', 'assistant') NOT NULL,
  content TEXT NOT NULL,
  model VARCHAR(80) NULL,
  page_context VARCHAR(100) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversation(conversation_id),
  INDEX idx_ai_chat_message_conversation (conversation_id, message_id)
);
