package com.anshun.dms.agent.tools;

import com.anshun.dms.agent.AgentTool;
import com.anshun.dms.agent.AgentToolContext;
import com.anshun.dms.agent.AgentToolOutput;
import com.anshun.dms.service.StatisticsService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DashboardOverviewTool implements AgentTool {
    private final StatisticsService statisticsService;
    public DashboardOverviewTool(StatisticsService statisticsService) { this.statisticsService = statisticsService; }

    @Override public String name() { return "get_dashboard_overview"; }
    @Override public String description() { return "查询广告点位总数、已出租数、出租率、累计收入和欠费金额。仅返回当前系统汇总数据。"; }
    @Override public String requiredPermission() { return "stats:view"; }
    @Override public Set<String> allowedArguments() { return Set.of(); }
    @Override public Map<String, Object> parameterSchema() { return Map.of("type", "object", "properties", Map.of(), "additionalProperties", false); }
    @Override public AgentToolOutput execute(JsonNode arguments, AgentToolContext context) {
        return new AgentToolOutput(statisticsService.overview(), "已查询数据概览核心指标");
    }
}
