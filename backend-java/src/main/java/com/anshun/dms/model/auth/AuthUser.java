package com.anshun.dms.model.auth;

import java.time.LocalDateTime;

/** Minimal authentication projection; password hashes never leave the service layer. */
public record AuthUser(
        long userId,
        String username,
        String passwordHash,
        String nickname,
        String role,
        String status,
        int tokenVersion,
        int failedLoginAttempts,
        LocalDateTime lockedUntil,
        LocalDateTime createTime) { }
