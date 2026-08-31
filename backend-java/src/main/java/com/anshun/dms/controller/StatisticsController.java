package com.anshun.dms.controller;

import com.anshun.dms.model.statistics.StatisticsModels;
import com.anshun.dms.service.StatisticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@PreAuthorize("hasAuthority('stats:view')")
public class StatisticsController {
    private final StatisticsService statisticsService;
    public StatisticsController(StatisticsService statisticsService) { this.statisticsService = statisticsService; }

    @GetMapping("/overview") public StatisticsModels.Overview overview() { return statisticsService.overview(); }
    @GetMapping("/revenue-trend") public List<StatisticsModels.RevenueTrend> revenueTrend(@RequestParam(required = false) Integer year) { return statisticsService.revenueTrend(year); }
    @GetMapping("/industry-distribution") public List<StatisticsModels.IndustryDistribution> industryDistribution() { return statisticsService.industryDistribution(); }
    @GetMapping("/hot-areas") public List<StatisticsModels.HotArea> hotAreas() { return statisticsService.hotAreas(); }
    @GetMapping("/map-positions") public List<StatisticsModels.MapPosition> mapPositions() { return statisticsService.mapPositions(); }
    @GetMapping("/top-enterprises") public List<StatisticsModels.TopEnterprise> topEnterprises() { return statisticsService.topEnterprises(); }
}
