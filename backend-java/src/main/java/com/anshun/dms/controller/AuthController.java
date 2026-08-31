package com.anshun.dms.controller;

import com.anshun.dms.audit.OperationLog;
import com.anshun.dms.common.ApiResponse;
import com.anshun.dms.dto.LoginRequest;
import com.anshun.dms.dto.RegisterRequest;
import com.anshun.dms.service.AuthService;
import com.anshun.dms.security.ClientIpResolver;
import com.anshun.dms.vo.CurrentUserVO;
import com.anshun.dms.vo.LoginVO;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final ClientIpResolver clientIpResolver;

    public AuthController(AuthService authService, ClientIpResolver clientIpResolver) {
        this.authService = authService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/register")
    @OperationLog(module = "认证", action = "注册账号", target = "#request.username")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.successMessage("注册成功");
    }

    @PostMapping("/login")
    @OperationLog(module = "认证", action = "登录", target = "#request.username")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.login(request, clientIpResolver.resolve(servletRequest)));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserVO> me(Authentication authentication) {
        return ApiResponse.success(authService.currentUser(authentication.getName()));
    }

    @PostMapping("/logout")
    @OperationLog(module = "认证", action = "退出登录")
    public ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return ApiResponse.successMessage("已退出登录");
    }
}
