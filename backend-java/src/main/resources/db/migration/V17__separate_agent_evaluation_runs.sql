ALTER TABLE ai_agent_run
  ADD COLUMN run_type VARCHAR(20) NOT NULL DEFAULT 'ONLINE' AFTER request_id,
  ADD CONSTRAINT chk_ai_agent_run_type CHECK (run_type IN ('ONLINE', 'EVALUATION'));

CREATE INDEX idx_ai_agent_run_type_recent ON ai_agent_run (run_type, create_time DESC);
