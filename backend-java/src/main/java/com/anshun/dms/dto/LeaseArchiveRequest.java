package com.anshun.dms.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Carries the version observed by the operator so archive participates in optimistic locking. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LeaseArchiveRequest(
        @NotNull(message = "数据版本不能为空")
        @Min(value = 0, message = "数据版本不合法") Integer version) { }
