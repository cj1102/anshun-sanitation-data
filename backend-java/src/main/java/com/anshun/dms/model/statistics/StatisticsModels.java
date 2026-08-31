package com.anshun.dms.model.statistics;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.io.Serializable;

/** API response models for the operations dashboard. */
public final class StatisticsModels {
    private StatisticsModels() { }

    public record Overview(long totalPositions, long leasedPositions, BigDecimal leasedRate,
                           BigDecimal totalRevenue, BigDecimal arrearsAmount) implements Serializable { }

    public record RevenueTrend(Integer year, Integer month, BigDecimal revenue) implements Serializable { }

    public record IndustryDistribution(@JsonProperty("industry_name") String industryName,
                                       @JsonProperty("total_rent") BigDecimal totalRent,
                                       @JsonProperty("total_pos") long totalPositions) implements Serializable { }

    public record HotArea(@JsonProperty("area_name") String areaName,
                          @JsonProperty("rent_rate") BigDecimal rentRate,
                          @JsonProperty("avg_rent") BigDecimal averageRent,
                          @JsonProperty("turnover_rate") BigDecimal turnoverRate) implements Serializable { }

    public record MapPosition(@JsonProperty("ad_position_code") String positionCode,
                              @JsonProperty("ad_location") String location,
                              BigDecimal longitude, BigDecimal latitude, String status,
                              @JsonProperty("total_ad_area") int totalArea,
                              @JsonProperty("ad_specification") String specification,
                              String district,
                              @JsonProperty("road_name") String roadName) implements Serializable { }

    public record TopEnterprise(@JsonProperty("lessee_company") String company,
                                @JsonProperty("total_rent") BigDecimal totalRent) implements Serializable { }
}
