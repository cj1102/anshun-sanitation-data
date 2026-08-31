package com.anshun.dms.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PositionSaveRequest(
        @NotBlank(message = "点位编码不能为空") String adPositionCode,
        @NotBlank(message = "点位设立位置不能为空") String adLocation,
        @NotBlank(message = "单面面积说明不能为空") String singleSideArea,
        @NotNull(message = "点位总面积不能为空") @Positive(message = "点位总面积必须大于 0") Integer totalAdArea,
        @NotBlank(message = "点位规格不能为空") String adSpecification,
        BigDecimal longitude, BigDecimal latitude, String district, String roadName, String status, String remark,
        @Min(value = 0, message = "数据版本不合法") Integer version) { }
