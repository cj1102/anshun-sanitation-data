package com.anshun.dms.service;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.dto.LoginRequest;
import com.anshun.dms.mapper.AuthMapper;
import com.anshun.dms.model.auth.AuthUser;
import com.anshun.dms.security.JwtTokenService;
import com.anshun.dms.security.LoginRateLimitService;
import com.anshun.dms.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock AuthMapper authMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenService jwtTokenService;
    @Mock RbacService rbacService;
    @Mock ObjectProvider<TokenBlacklistService> blacklistProvider;
    @Mock LoginRateLimitService loginRateLimitService;
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(authMapper, passwordEncoder, jwtTokenService, rbacService, blacklistProvider,
                loginRateLimitService, true);
    }

    @Test
    void loginIssuesTokenWithCurrentSecurityVersion() {
        AuthUser user = user(7, "active", 3, 0, null);
        when(authMapper.findByUsername("alice")).thenReturn(user);
        when(passwordEncoder.matches("Password1", user.passwordHash())).thenReturn(true);
        when(rbacService.roleCodes(7)).thenReturn(List.of("OPERATOR"));
        when(rbacService.permissionCodes(7)).thenReturn(List.of("position:view"));
        when(jwtTokenService.createToken(7, "alice", List.of("OPERATOR"), List.of("position:view"), 3))
                .thenReturn("signed-token");

        var result = service.login(new LoginRequest("alice", "Password1"), "127.0.0.1");

        assertThat(result.token()).isEqualTo("signed-token");
        assertThat(result.user().roles()).containsExactly("OPERATOR");
        verify(authMapper).resetLoginFailures(7);
    }

    @Test
    void wrongPasswordPersistsFailureBeforeRejectingLogin() {
        AuthUser user = user(7, "active", 0, 2, null);
        when(authMapper.findByUsername("alice")).thenReturn(user);
        when(passwordEncoder.matches("wrong", user.passwordHash())).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("alice", "wrong"), "127.0.0.1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.status()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(authMapper).recordLoginFailure(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.eq(5), any(LocalDateTime.class));
    }

    @Test
    void lockedAccountIsRateLimited() {
        when(authMapper.findByUsername("alice"))
                .thenReturn(user(7, "active", 0, 5, LocalDateTime.now().plusMinutes(10)));

        assertThatThrownBy(() -> service.login(new LoginRequest("alice", "Password1"), "127.0.0.1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    private AuthUser user(long id, String status, int tokenVersion, int failures, LocalDateTime lockedUntil) {
        return new AuthUser(id, "alice", "$2a$10$hash", "Alice", "user", status, tokenVersion,
                failures, lockedUntil, LocalDateTime.now());
    }
}
