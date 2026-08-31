package com.anshun.dms.agent.tools;

import com.anshun.dms.agent.AgentTool;
import com.anshun.dms.agent.AgentToolArguments;
import com.anshun.dms.agent.AgentToolContext;
import com.anshun.dms.agent.AgentToolOutput;
import com.anshun.dms.agent.ToolSideEffect;
import com.anshun.dms.dto.PositionSaveRequest;
import com.anshun.dms.service.AiPendingActionService;
import com.anshun.dms.vo.AiPendingActionVO;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Creates a confirmation draft only; it never writes t_ad_position. */
@Component
public class PrepareCreateAdPositionTool implements AgentTool {
    private final AiPendingActionService pendingActionService;

    public PrepareCreateAdPositionTool(AiPendingActionService pendingActionService) {
        this.pendingActionService = pendingActionService;
    }

    @Override public String name() { return "prepare_create_ad_position"; }
    @Override public String description() {
        return "根据用户已明确提供的完整信息生成“新增广告点位”待确认草稿。此工具绝不创建真实点位；"
                + "必须由用户在界面中再次确认后才会执行。缺少必填字段时先向用户追问，不能调用此工具。";
    }
    @Override public String requiredPermission() { return "position:create"; }
    @Override public ToolSideEffect sideEffect() { return ToolSideEffect.WRITE; }
    @Override public Set<String> allowedArguments() {
        return Set.of("adPositionCode", "adLocation", "singleSideArea", "totalAdArea", "adSpecification",
                "district", "roadName", "longitude", "latitude", "remark");
    }
    @Override public Map<String, Object> parameterSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("adPositionCode", Map.of("type", "string", "description", "唯一广告点位编码，例如 AS-NEW-001"));
        properties.put("adLocation", Map.of("type", "string", "description", "广告点位具体设立位置"));
        properties.put("singleSideArea", Map.of("type", "string", "description", "单面面积说明，例如 6m×3m"));
        properties.put("totalAdArea", Map.of("type", "integer", "minimum", 1, "description", "总广告面积，单位平方米"));
        properties.put("adSpecification", Map.of("type", "string", "description", "广告规格，例如 双面立柱式"));
        properties.put("district", Map.of("type", "string", "description", "所在区县，可选"));
        properties.put("roadName", Map.of("type", "string", "description", "道路名称，可选"));
        properties.put("longitude", Map.of("type", "number", "description", "经度，可选"));
        properties.put("latitude", Map.of("type", "number", "description", "纬度，可选"));
        properties.put("remark", Map.of("type", "string", "description", "备注，可选"));
        return Map.of("type", "object", "properties", properties,
                "required", List.of("adPositionCode", "adLocation", "singleSideArea", "totalAdArea", "adSpecification"),
                "additionalProperties", false);
    }
    @Override public AgentToolOutput execute(JsonNode arguments, AgentToolContext context) {
        PositionSaveRequest request = new PositionSaveRequest(
                AgentToolArguments.requiredIdentifier(arguments, "adPositionCode"),
                AgentToolArguments.optionalText(arguments, "adLocation", 200),
                AgentToolArguments.optionalText(arguments, "singleSideArea", 80),
                AgentToolArguments.requiredPositiveInt(arguments, "totalAdArea"),
                AgentToolArguments.optionalText(arguments, "adSpecification", 100),
                AgentToolArguments.optionalDecimal(arguments, "longitude"),
                AgentToolArguments.optionalDecimal(arguments, "latitude"),
                AgentToolArguments.optionalText(arguments, "district", 80),
                AgentToolArguments.optionalText(arguments, "roadName", 120),
                "vacant", AgentToolArguments.optionalText(arguments, "remark", 500), null);
        AiPendingActionVO action = pendingActionService.prepareCreatePosition(context.username(), context.agentRunId(),
                context.pageContext(), request);
        return new AgentToolOutput(Map.of("actionId", action.actionId(), "requiresUserConfirmation", true,
                        "expiresAt", action.expiresAt().toString(), "preview", action.fields()),
                "已生成新增广告点位草稿，等待用户在界面确认", action);
    }
}
