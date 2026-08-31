INSERT IGNORE INTO sys_permission (permission_code, permission_name, module_name, description) VALUES
  ('ai:evaluation:view', '查看 AI 评测', 'AI 助手', '查看 Agent 运行质量、反馈与离线评测结果'),
  ('ai:evaluation:manage', '管理 AI 评测', 'AI 助手', '维护并运行 Agent 离线评测用例');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id FROM sys_role r JOIN sys_permission p
WHERE r.role_code='ADMIN' AND p.permission_code IN ('ai:evaluation:view', 'ai:evaluation:manage');

CREATE TABLE IF NOT EXISTS ai_agent_evaluation_case (
  case_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  case_name VARCHAR(120) NOT NULL,
  question VARCHAR(2000) NOT NULL,
  page_context VARCHAR(100) NOT NULL DEFAULT '/dashboard',
  expected_tool_name VARCHAR(64) NULL,
  expected_keywords VARCHAR(500) NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  creator_id INT NOT NULL,
  creator_username VARCHAR(50) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_agent_evaluation_case_user FOREIGN KEY (creator_id) REFERENCES t_user(user_id),
  INDEX idx_ai_agent_evaluation_case_enabled (enabled, case_id DESC)
);

CREATE TABLE IF NOT EXISTS ai_agent_evaluation_result (
  result_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  case_id BIGINT NOT NULL,
  agent_run_id BIGINT NULL,
  model VARCHAR(80) NULL,
  actual_tools VARCHAR(500) NULL,
  expected_tool_matched TINYINT NOT NULL,
  expected_keywords_matched TINYINT NOT NULL,
  passed TINYINT NOT NULL,
  detail VARCHAR(1000) NOT NULL,
  duration_ms BIGINT NOT NULL,
  evaluator_username VARCHAR(50) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_agent_evaluation_result_case FOREIGN KEY (case_id) REFERENCES ai_agent_evaluation_case(case_id),
  CONSTRAINT fk_ai_agent_evaluation_result_run FOREIGN KEY (agent_run_id) REFERENCES ai_agent_run(run_id),
  INDEX idx_ai_agent_evaluation_result_case (case_id, result_id DESC),
  INDEX idx_ai_agent_evaluation_result_recent (create_time DESC),
  INDEX idx_ai_agent_evaluation_result_passed (passed, create_time DESC)
);

INSERT INTO ai_agent_evaluation_case (case_name, question, page_context, expected_tool_name, expected_keywords, creator_id, creator_username)
SELECT '数据概览工具回归', '当前广告点位总数、已租数量和空置数量分别是多少？', '/dashboard',
  'get_dashboard_overview', '广告,点位', u.user_id, u.username
FROM t_user u
WHERE u.username='admin'
  AND NOT EXISTS (SELECT 1 FROM ai_agent_evaluation_case c WHERE c.case_name='数据概览工具回归');
