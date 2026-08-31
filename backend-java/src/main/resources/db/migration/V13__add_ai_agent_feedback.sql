CREATE TABLE IF NOT EXISTS ai_agent_feedback (
  feedback_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  run_id BIGINT NOT NULL,
  user_id INT NOT NULL,
  rating ENUM('UP', 'DOWN') NOT NULL,
  comment VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_agent_feedback_run FOREIGN KEY (run_id) REFERENCES ai_agent_run(run_id),
  CONSTRAINT fk_ai_agent_feedback_user FOREIGN KEY (user_id) REFERENCES t_user(user_id),
  UNIQUE KEY uk_ai_agent_feedback_run_user (run_id, user_id),
  INDEX idx_ai_agent_feedback_recent (create_time DESC),
  INDEX idx_ai_agent_feedback_rating (rating, create_time DESC)
);
