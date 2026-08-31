package com.anshun.dms.controller;

import com.anshun.dms.audit.OperationLog;
import com.anshun.dms.common.ApiResponse;
import com.anshun.dms.dto.RoleAssignmentRequest;
import com.anshun.dms.service.SystemUserService;
import com.anshun.dms.vo.PageData;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@PreAuthorize("hasAuthority('system:user:manage')")
public class SystemUserController {
    private final SystemUserService systemUserService;

    public SystemUserController(SystemUserService systemUserService) { this.systemUserService = systemUserService; }

    @GetMapping("/users")
    public ApiResponse<PageData<Map<String, Object>>> users(@RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "20") int limit,
                                                            @RequestParam(required = false) String search) {
        return ApiResponse.success(systemUserService.users(page, limit, search));
    }

    @GetMapping("/roles")
    public ApiResponse<List<Map<String, Object>>> roles() { return ApiResponse.success(systemUserService.roles()); }

    @GetMapping("/permissions")
    public ApiResponse<List<Map<String, Object>>> permissions() {
        return ApiResponse.success(systemUserService.permissions());
    }

    @PutMapping("/users/{userId}/roles")
    @OperationLog(module = "系统管理", action = "分配用户角色", target = "#userId")
    public ApiResponse<Void> assignRoles(@PathVariable long userId,
                                         @Valid @RequestBody RoleAssignmentRequest request) {
        systemUserService.assignRoles(userId, request.roleCodes());
        return ApiResponse.successMessage("用户角色已更新，旧登录状态已失效");
    }
}
