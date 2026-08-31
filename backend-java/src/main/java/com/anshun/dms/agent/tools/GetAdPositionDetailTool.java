package com.anshun.dms.agent.tools;

import com.anshun.dms.agent.AgentTool;
import com.anshun.dms.agent.AgentToolContext;
import com.anshun.dms.agent.AgentToolArguments;
import com.anshun.dms.agent.AgentToolOutput;
import com.anshun.dms.service.PositionService;
import com.anshun.dms.vo.PositionDetailVO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class GetAdPositionDetailTool implements AgentTool {
    private final PositionService positionService;
    public GetAdPositionDetailTool(PositionService positionService) { this.positionService = positionService; }

    @Override public String name() { return "get_ad_position_detail"; }
    @Override public String description() { return "根据广告点位编码查询点位详情、估值和近期租赁历史。仅在用户已提供或已查询到点位编码时调用。"; }
    @Override public String requiredPermission() { return "position:view"; }
    @Override public Set<String> allowedArguments() { return Set.of("adPositionCode"); }
    @Override public Map<String, Object> parameterSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "adPositionCode", Map.of("type", "string", "description", "广告点位编码，例如 AS-COL-039")
        ), "required", List.of("adPositionCode"), "additionalProperties", false);
    }
    @Override public AgentToolOutput execute(JsonNode arguments, AgentToolContext context) {
        String code = AgentToolArguments.requiredIdentifier(arguments, "adPositionCode");
        PositionDetailVO detail = positionService.detail(code);
        return new AgentToolOutput(detail, "已查询广告点位 " + code + " 的详情");
    }
}
