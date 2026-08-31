package com.anshun.dms.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AuditLogService {
    private final JdbcTemplate jdbc;
    public AuditLogService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(AuditEntry entry) {
        jdbc.update("""
                INSERT INTO sys_operation_log (operator_id, operator_username, module_name, action_name, target_id,
                  request_method, request_path, request_id, client_ip, success, error_message, duration_ms)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, entry.operatorId(), entry.username(), entry.module(), entry.action(), entry.targetId(),
                entry.requestMethod(), entry.requestPath(), entry.requestId(), entry.clientIp(), entry.success(),
                entry.errorMessage(), entry.durationMs());
    }

    @Transactional(readOnly = true)
    public AuditLogPage list(int requestedPage, int requestedLimit, String module, String username) {
        int page = Math.max(1, requestedPage);
        int limit = Math.min(100, Math.max(1, requestedLimit));
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (module != null && !module.isBlank()) {
            where.append(" AND module_name=?");
            args.add(module.trim());
        }
        if (username != null && !username.isBlank()) {
            where.append(" AND operator_username LIKE ?");
            args.add("%" + username.trim() + "%");
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM sys_operation_log" + where, Long.class, args.toArray());
        args.add(limit);
        args.add((page - 1) * limit);
        List<Map<String, Object>> data = jdbc.queryForList("""
                SELECT log_id, operator_username, module_name, action_name, target_id, request_id,
                  client_ip, success, error_message, duration_ms, create_time
                FROM sys_operation_log
                """ + where + " ORDER BY log_id DESC LIMIT ? OFFSET ?", args.toArray());
        return new AuditLogPage(total == null ? 0 : total, data);
    }

    public record AuditEntry(Integer operatorId, String username, String module, String action, String targetId,
                             String requestMethod, String requestPath, String requestId, String clientIp,
                             boolean success, String errorMessage, long durationMs) { }

    public record AuditLogPage(long total, List<Map<String, Object>> data) { }
}
