package com.anshun.dms.service;

import com.anshun.dms.mapper.AuthMapper;
import com.anshun.dms.model.auth.UserTokenState;
import org.springframework.stereotype.Service;

@Service
public class UserTokenStateService {
    private final AuthMapper authMapper;

    public UserTokenStateService(AuthMapper authMapper) { this.authMapper = authMapper; }

    public boolean isCurrent(long userId, String username, int tokenVersion) {
        UserTokenState state = authMapper.findTokenState(userId);
        return state != null && "active".equals(state.status()) && state.username().equals(username)
                && state.tokenVersion() == tokenVersion;
    }
}
