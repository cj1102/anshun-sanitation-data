CREATE TABLE IF NOT EXISTS ai_agent_run (
  run_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  conversation_id BIGINT NULL,
  model VARCHAR(80) NOT NULL,
  page_context VARCHAR(100) NULL,
  request_id VARCHAR(80) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
  tool_call_count INT NOT NULL DEFAULT 0,
  duration_ms BIGINT NULL,
  error_message VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  complete_time DATETIME NULL,
  CONSTRAINT fk_ai_agent_run_user FOREIGN KEY (user_id) REFERENCES t_user(user_id),
  CONSTRAINT fk_ai_agent_run_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversation(conversation_id),
  INDEX idx_ai_agent_run_user_recent (user_id, create_time DESC),
  INDEX idx_ai_agent_run_request (request_id)
);

CREATE TABLE IF NOT EXISTS ai_agent_tool_call (
  agent_tool_call_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  run_id BIGINT NOT NULL,
  sequence_no INT NOT NULL,
  provider_call_id VARCHAR(128) NULL,
  tool_name VARCHAR(64) NOT NULL,
  arguments_summary VARCHAR(1000) NULL,
  result_summary TEXT NULL,
  success TINYINT NOT NULL,
  duration_ms BIGINT NOT NULL,
  error_message VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_agent_tool_call_run FOREIGN KEY (run_id) REFERENCES ai_agent_run(run_id),
  UNIQUE KEY uk_ai_agent_tool_call_sequence (run_id, sequence_no),
  INDEX idx_ai_agent_tool_call_run (run_id, agent_tool_call_id)
);
