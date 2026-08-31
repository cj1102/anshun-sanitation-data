package com.anshun.dms.service;

import com.anshun.dms.model.statistics.StatisticsModels;
import com.anshun.dms.repository.StatisticsRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StatisticsService {
    private final StatisticsRepository repository;

    public StatisticsService(StatisticsRepository repository) { this.repository = repository; }

    @Cacheable(cacheNames = "statistics", key = "'overview'")
    public StatisticsModels.Overview overview() {
        long total = repository.countPositions();
        long leased = repository.countLeasedPositions();
        BigDecimal rate = total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(leased * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
        return new StatisticsModels.Overview(total, leased, rate, repository.totalRevenue(), repository.arrearsAmount());
    }

    @Cacheable(cacheNames = "statistics", key = "'revenue-trend:' + (#year == null ? 'all' : #year)")
    public List<StatisticsModels.RevenueTrend> revenueTrend(Integer year) { return repository.revenueTrend(year); }
    @Cacheable(cacheNames = "statistics", key = "'industry-distribution'")
    public List<StatisticsModels.IndustryDistribution> industryDistribution() { return repository.industryDistribution(); }
    @Cacheable(cacheNames = "statistics", key = "'hot-areas'")
    public List<StatisticsModels.HotArea> hotAreas() { return repository.hotAreas(); }
    @Cacheable(cacheNames = "statistics", key = "'map-positions'")
    public List<StatisticsModels.MapPosition> mapPositions() { return repository.mapPositions(); }
    @Cacheable(cacheNames = "statistics", key = "'top-enterprises'")
    public List<StatisticsModels.TopEnterprise> topEnterprises() { return repository.topEnterprises(); }
}
