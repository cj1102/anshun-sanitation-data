package com.anshun.dms.agent.tools;

import com.anshun.dms.agent.AgentTool;
import com.anshun.dms.agent.AgentToolContext;
import com.anshun.dms.agent.AgentToolArguments;
import com.anshun.dms.agent.AgentToolOutput;
import com.anshun.dms.dto.PositionPageQuery;
import com.anshun.dms.service.PositionService;
import com.anshun.dms.vo.PageData;
import com.anshun.dms.vo.PositionVO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class SearchAdPositionsTool implements AgentTool {
    private final PositionService positionService;
    public SearchAdPositionsTool(PositionService positionService) { this.positionService = positionService; }

    @Override public String name() { return "search_ad_positions"; }
    @Override public String description() { return "按点位编码、位置、道路、区县或租赁状态查询广告点位。适合回答某地有哪些点位、点位是否出租等问题。"; }
    @Override public String requiredPermission() { return "position:view"; }
    @Override public Set<String> allowedArguments() { return Set.of("search", "district", "status", "limit"); }
    @Override public Map<String, Object> parameterSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "search", Map.of("type", "string", "description", "点位编码、位置或道路关键字"),
                "district", Map.of("type", "string", "description", "区县名称"),
                "status", Map.of("type", "string", "enum", List.of("leased", "vacant"), "description", "leased 表示已出租，vacant 表示空置"),
                "limit", Map.of("type", "integer", "minimum", 1, "maximum", 10, "description", "返回数量，默认 5")
        ), "additionalProperties", false);
    }
    @Override public AgentToolOutput execute(JsonNode arguments, AgentToolContext context) {
        PositionPageQuery query = new PositionPageQuery();
        query.setPage(1);
        query.setLimit(AgentToolArguments.limit(arguments, 5, 10));
        query.setSearch(AgentToolArguments.optionalText(arguments, "search", 60));
        query.setDistrict(AgentToolArguments.optionalText(arguments, "district", 40));
        query.setStatus(AgentToolArguments.enumValue(arguments, "status", Set.of("leased", "vacant")));
        PageData<PositionVO> page = positionService.page(query);
        List<Map<String, Object>> items = page.data().stream().map(this::summary).toList();
        return new AgentToolOutput(Map.of("total", page.total(), "items", items), "已查询到 " + page.total() + " 个匹配广告点位，返回前 " + items.size() + " 条");
    }

    private Map<String, Object> summary(PositionVO position) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("adPositionCode", position.adPositionCode()); item.put("adLocation", position.adLocation());
        item.put("district", position.district()); item.put("roadName", position.roadName());
        item.put("status", position.status()); item.put("totalAdArea", position.totalAdArea());
        return item;
    }
}
