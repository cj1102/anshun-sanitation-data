package com.anshun.dms.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LeaseVO(Long adLeaseId, String contractCode, String adPositionCode, String adLocation, String singleSideArea,
                      Integer totalAdArea, String adSpecification, String lesseeCode, String lesseeCompany,
                      BigDecimal leaseRent, Integer leaseTerm, LocalDate leaseStartDate, LocalDate leaseEndDate,
                      LocalDate contractSignDate, Integer version, Long attachmentCount,
                      String approvalStatus, String submittedByUsername, LocalDateTime submittedAt,
                      String approverUsername, LocalDateTime approvedAt, String approvalComment) { }
