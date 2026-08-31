package com.anshun.dms.vo;

import java.time.LocalDateTime;

public record CurrentUserVO(
        long userId,
        String username,
        String nickname,
        String role,
        String status,
        LocalDateTime createTime) { }
