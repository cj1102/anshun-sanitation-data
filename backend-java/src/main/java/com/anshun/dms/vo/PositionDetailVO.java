package com.anshun.dms.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PositionDetailVO(Long adPositionId, String adPositionCode, String adLocation, String singleSideArea,
                               Integer totalAdArea, String adSpecification, BigDecimal longitude, BigDecimal latitude,
                               String district, String roadName, String status, String remark, Integer version,
                               PositionValuationVO valuation, List<LeaseVO> leaseHistory) { }
