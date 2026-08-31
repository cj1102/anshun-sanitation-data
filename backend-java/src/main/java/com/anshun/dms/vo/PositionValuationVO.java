package com.anshun.dms.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PositionValuationVO(Long valuationId, String adPositionCode, String adLocation, String singleSideArea,
                                  Integer totalAdArea, String adSpecification, Integer adCount, BigDecimal discountedRent,
                                  BigDecimal totalAssessedValue, LocalDate valuationDate, String lesseeCompany,
                                  String valuationMethod) { }
