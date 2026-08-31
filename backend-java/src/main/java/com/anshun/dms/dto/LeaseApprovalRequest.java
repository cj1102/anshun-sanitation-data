package com.anshun.dms.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LeaseApprovalRequest(
        @Size(max = 500, message = "审核意见不能超过 500 个字符") String comment) { }
