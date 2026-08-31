package com.anshun.dms.controller;

import com.anshun.dms.audit.AuditLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/audit-logs")
@PreAuthorize("hasAuthority('system:audit:view')")
public class AuditLogController {
    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public AuditLogService.AuditLogPage list(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int limit,
                                             @RequestParam(required = false) String module,
                                             @RequestParam(required = false) String username) {
        return auditLogService.list(page, limit, module, username);
    }
}
