package com.anshun.dms.service;

import com.anshun.dms.mapper.AuthMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RbacService {
    private final AuthMapper authMapper;

    public RbacService(AuthMapper authMapper) {
        this.authMapper = authMapper;
    }

    public List<String> roleCodes(long userId) {
        return authMapper.selectRoleCodes(userId);
    }

    public List<String> permissionCodes(long userId) {
        return authMapper.selectPermissionCodes(userId);
    }
}
