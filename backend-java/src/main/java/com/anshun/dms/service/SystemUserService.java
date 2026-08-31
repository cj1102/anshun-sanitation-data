package com.anshun.dms.service;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.mapper.SystemUserMapper;
import com.anshun.dms.vo.PageData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SystemUserService {
    private final SystemUserMapper systemUserMapper;

    public SystemUserService(SystemUserMapper systemUserMapper) { this.systemUserMapper = systemUserMapper; }

    public PageData<Map<String, Object>> users(int page, int limit, String search) {
        int safePage = Math.max(1, page);
        int safeLimit = Math.min(100, Math.max(1, limit));
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return new PageData<>(systemUserMapper.countUsers(normalizedSearch),
                systemUserMapper.selectUsers(normalizedSearch, safeLimit, (safePage - 1) * safeLimit));
    }

    public List<Map<String, Object>> roles() { return systemUserMapper.selectRoles(); }
    public List<Map<String, Object>> permissions() { return systemUserMapper.selectPermissions(); }

    @Transactional
    public void assignRoles(long userId, List<String> requestedRoleCodes) {
        if (systemUserMapper.countUser(userId) == 0) throw BusinessException.notFound("用户不存在");
        List<String> roleCodes = requestedRoleCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .distinct().toList();
        if (roleCodes.isEmpty()) throw BusinessException.badRequest("至少需要分配一个角色");
        if (systemUserMapper.countActiveRoles(roleCodes) != roleCodes.size()) {
            throw BusinessException.badRequest("包含不存在或已停用的角色编码");
        }
        List<Long> adminUserIds = systemUserMapper.selectAdminUserIdsForUpdate();
        if (adminUserIds.contains(userId) && adminUserIds.size() == 1 && !roleCodes.contains("ADMIN")) {
            throw BusinessException.conflict("不能移除系统中最后一个管理员角色");
        }
        systemUserMapper.deleteUserRoles(userId);
        if (systemUserMapper.insertUserRoles(userId, roleCodes) != roleCodes.size()) {
            throw BusinessException.conflict("角色分配未完整写入，请重试");
        }
        if (systemUserMapper.incrementTokenVersion(userId) != 1) {
            throw BusinessException.conflict("用户安全版本更新失败，请重试");
        }
    }
}
