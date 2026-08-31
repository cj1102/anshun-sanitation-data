package com.anshun.dms.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RoleAssignmentRequest(
        @NotEmpty(message = "至少需要分配一个角色") List<String> roleCodes) { }
