package com.anshun.dms.vo;

import java.util.List;

public record SessionUserVO(
        long userId,
        String username,
        String nickname,
        String role,
        List<String> roles,
        List<String> permissions) { }
