package com.anshun.dms.repository;

import com.anshun.dms.model.statistics.StatisticsModels;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class StatisticsRepository {
    private final JdbcTemplate jdbc;

    public StatisticsRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long countPositions() { return number("SELECT COUNT(*) FROM t_ad_position WHERE deleted=0").longValue(); }
    public long countLeasedPositions() { return number("""
            SELECT COUNT(DISTINCT l.ad_position_code) FROM t_ad_lease_detail l JOIN t_ad_position p ON p.ad_position_code=l.ad_position_code
            WHERE l.deleted=0 AND l.approval_status='APPROVED' AND p.deleted=0 AND CURRENT_DATE BETWEEN l.lease_start_date AND l.lease_end_date
            """).longValue(); }
    public BigDecimal totalRevenue() { return decimal("SELECT COALESCE(SUM(lease_rent), 0) FROM t_ad_lease_detail WHERE deleted=0 AND approval_status='APPROVED'"); }
    public BigDecimal arrearsAmount() { return decimal("""
            SELECT COALESCE(SUM(period_rent), 0) FROM t_ad_revenue_stat WHERE rent_status IN ('欠费', '待收回')
            """); }

    public List<StatisticsModels.RevenueTrend> revenueTrend(Integer year) {
        if (year == null) return jdbc.query("""
                SELECT stat_year, SUM(period_rent) FROM t_ad_revenue_stat
                WHERE stat_cycle='year' GROUP BY stat_year ORDER BY stat_year
                """, (rs, row) -> new StatisticsModels.RevenueTrend(rs.getInt(1), null, rs.getBigDecimal(2)));
        return jdbc.query("""
                SELECT stat_month, SUM(period_rent) FROM t_ad_revenue_stat
                WHERE stat_cycle='month' AND stat_year=? GROUP BY stat_month ORDER BY stat_month
                """, (rs, row) -> new StatisticsModels.RevenueTrend(null, rs.getInt(1), rs.getBigDecimal(2)), year);
    }

    public List<StatisticsModels.IndustryDistribution> industryDistribution() {
        return jdbc.query("""
                SELECT industry_name, SUM(total_rent_contribution), SUM(total_ad_position)
                FROM t_lease_enterprise_industry_dist
                WHERE statistic_dimension='year' AND stat_interval_start=(
                  SELECT MAX(stat_interval_start) FROM t_lease_enterprise_industry_dist WHERE statistic_dimension='year'
                )
                GROUP BY industry_name ORDER BY SUM(total_rent_contribution) DESC
                """, (rs, row) -> new StatisticsModels.IndustryDistribution(rs.getString(1), rs.getBigDecimal(2), rs.getLong(3)));
    }

    public List<StatisticsModels.HotArea> hotAreas() {
        return jdbc.query("""
                SELECT area_name, ROUND(AVG(ad_position_rent_rate),2), ROUND(AVG(avg_rent_per_sqm),2), ROUND(AVG(ad_turnover_rate),2)
                FROM t_ad_hot_area_analysis GROUP BY area_name ORDER BY AVG(ad_position_rent_rate) DESC
                """, (rs, row) -> new StatisticsModels.HotArea(rs.getString(1), rs.getBigDecimal(2), rs.getBigDecimal(3), rs.getBigDecimal(4)));
    }

    public List<StatisticsModels.MapPosition> mapPositions() {
        return jdbc.query("""
                SELECT p.ad_position_code, p.ad_location, p.longitude, p.latitude,
                  CASE WHEN EXISTS (SELECT 1 FROM t_ad_lease_detail l WHERE l.ad_position_code=p.ad_position_code AND l.deleted=0
                    AND l.approval_status='APPROVED' AND CURRENT_DATE BETWEEN l.lease_start_date AND l.lease_end_date) THEN 'leased' ELSE 'vacant' END,
                  p.total_ad_area, p.ad_specification, p.district, p.road_name FROM t_ad_position p
                WHERE p.deleted=0
                """, (rs, row) -> new StatisticsModels.MapPosition(rs.getString(1), rs.getString(2), rs.getBigDecimal(3),
                rs.getBigDecimal(4), rs.getString(5), rs.getInt(6), rs.getString(7), rs.getString(8), rs.getString(9)));
    }

    public List<StatisticsModels.TopEnterprise> topEnterprises() {
        return jdbc.query("""
                SELECT lessee_company, SUM(total_rent_input) FROM t_lease_enterprise_ad_fund_stat
                WHERE statistic_dimension='year' AND stat_interval_start=(
                  SELECT MAX(stat_interval_start) FROM t_lease_enterprise_ad_fund_stat WHERE statistic_dimension='year'
                )
                GROUP BY lessee_company ORDER BY SUM(total_rent_input) DESC LIMIT 10
                """, (rs, row) -> new StatisticsModels.TopEnterprise(rs.getString(1), rs.getBigDecimal(2)));
    }

    private Number number(String sql) { Number value = jdbc.queryForObject(sql, Number.class); return value == null ? 0 : value; }
    private BigDecimal decimal(String sql) { Number value = number(sql); return value instanceof BigDecimal decimal ? decimal : BigDecimal.valueOf(value.doubleValue()); }
}
