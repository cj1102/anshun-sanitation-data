package com.anshun.dms.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LeaseSaveRequest(
        @NotBlank(message = "合同编码不能为空") String contractCode,
        @NotBlank(message = "广告点位编码不能为空") String adPositionCode,
        @NotBlank(message = "承租单位编码不能为空") String lesseeCode,
        @NotBlank(message = "承租单位名称不能为空") String lesseeCompany,
        @NotNull(message = "合同租金不能为空") @Positive(message = "合同租金必须大于 0") BigDecimal leaseRent,
        @NotNull(message = "租赁开始日期不能为空") LocalDate leaseStartDate,
        @NotNull(message = "租赁结束日期不能为空") LocalDate leaseEndDate,
        @NotNull(message = "合同签订日期不能为空") LocalDate contractSignDate,
        @Min(value = 0, message = "数据版本不合法") Integer version) { }
