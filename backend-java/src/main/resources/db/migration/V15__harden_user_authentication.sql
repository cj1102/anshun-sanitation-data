ALTER TABLE t_user
  ADD COLUMN token_version INT NOT NULL DEFAULT 0 COMMENT 'Increment to invalidate issued JWTs',
  ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0 COMMENT 'Consecutive failed logins',
  ADD COLUMN locked_until DATETIME NULL COMMENT 'Temporary login lock expiration';

CREATE INDEX idx_user_status_token_version ON t_user (user_id, status, token_version);
