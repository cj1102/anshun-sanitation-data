package com.anshun.dms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "请输入用户名") @Size(max = 50, message = "用户名长度不能超过 50 位") String username,
        @NotBlank(message = "请输入密码") @Size(max = 72, message = "密码长度不能超过 72 位") String password) { }
