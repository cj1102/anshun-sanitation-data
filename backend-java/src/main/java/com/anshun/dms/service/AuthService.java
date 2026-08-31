package com.anshun.dms.service;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.dto.LoginRequest;
import com.anshun.dms.dto.RegisterRequest;
import com.anshun.dms.mapper.AuthMapper;
import com.anshun.dms.model.auth.AuthUser;
import com.anshun.dms.security.JwtTokenService;
import com.anshun.dms.security.LoginRateLimitService;
import com.anshun.dms.security.TokenBlacklistService;
import com.anshun.dms.vo.CurrentUserVO;
import com.anshun.dms.vo.LoginVO;
import com.anshun.dms.vo.SessionUserVO;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class AuthService {
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    private static final ZoneId APPLICATION_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$WgK7gIDO8YTT3lyfzCAqn.Tfq1CYG3ELdaHPpN5UKUEdS9Q8.cC1i";

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RbacService rbacService;
    private final ObjectProvider<TokenBlacklistService> tokenBlacklistService;
    private final LoginRateLimitService loginRateLimitService;
    private final boolean registrationEnabled;

    public AuthService(AuthMapper authMapper, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService,
                       RbacService rbacService, ObjectProvider<TokenBlacklistService> tokenBlacklistService,
                       LoginRateLimitService loginRateLimitService,
                       @Value("${app.security.registration-enabled:true}") boolean registrationEnabled) {
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.rbacService = rbacService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.loginRateLimitService = loginRateLimitService;
        this.registrationEnabled = registrationEnabled;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (!registrationEnabled) throw BusinessException.notFound("注册功能未开放");
        String username = request.username().trim();
        if (authMapper.findByUsername(username) != null) throw BusinessException.conflict("用户名已存在");
        String nickname = request.nickname() == null || request.nickname().isBlank() ? username : request.nickname().trim();
        try {
            authMapper.insertUser(username, passwordEncoder.encode(request.password()), nickname);
            if (authMapper.assignViewerRole(username) != 1) {
                throw BusinessException.unavailable("默认角色初始化失败，请稍后重试");
            }
        } catch (DuplicateKeyException exception) {
            throw BusinessException.conflict("用户名已存在");
        }
    }

    public LoginVO login(LoginRequest request, String clientIp) {
        loginRateLimitService.check(request.username(), clientIp);
        AuthUser user = authMapper.findByUsername(request.username().trim());
        if (user == null) {
            passwordEncoder.matches(request.password(), DUMMY_PASSWORD_HASH);
            throw badCredentials();
        }
        if (!"active".equals(user.status())) throw BusinessException.unauthorized("账户不可用，请联系管理员");
        LocalDateTime now = LocalDateTime.now(APPLICATION_ZONE);
        if (user.lockedUntil() != null && user.lockedUntil().isAfter(now)) {
            throw BusinessException.tooManyRequests("登录失败次数过多，请 15 分钟后重试");
        }
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            authMapper.recordLoginFailure(user.userId(), MAX_LOGIN_ATTEMPTS, now.plusMinutes(LOCK_MINUTES));
            throw badCredentials();
        }

        authMapper.resetLoginFailures(user.userId());
        List<String> roles = rbacService.roleCodes(user.userId());
        List<String> permissions = rbacService.permissionCodes(user.userId());
        String primaryRole = roles.isEmpty() ? "VIEWER" : roles.get(0);
        String nickname = user.nickname() == null || user.nickname().isBlank() ? user.username() : user.nickname();
        String token = jwtTokenService.createToken(user.userId(), user.username(), roles, permissions, user.tokenVersion());
        return new LoginVO(token, new SessionUserVO(user.userId(), user.username(), nickname, primaryRole, roles, permissions));
    }

    public CurrentUserVO currentUser(String username) {
        AuthUser user = authMapper.findByUsername(username);
        if (user == null) throw BusinessException.notFound("用户不存在");
        return new CurrentUserVO(user.userId(), user.username(), user.nickname(), user.role(), user.status(), user.createTime());
    }

    public void logout(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw BusinessException.badRequest("缺少 Bearer Token");
        }
        TokenBlacklistService blacklist = tokenBlacklistService.getIfAvailable();
        if (blacklist != null) blacklist.revoke(authorization.substring(7));
    }

    private BusinessException badCredentials() { return BusinessException.unauthorized("用户名或密码错误"); }
}
