package com.anshun.dms.agent;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.vo.AiToolCallVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Model-visible tools are permission-filtered; execution repeats authorization defensively. */
@Service
public class AgentToolRegistry {
    public static final String READ_ONLY_AUTHORITY = "agent:mode:read-only";
    private final Map<String, AgentTool> tools;
    private final ObjectMapper objectMapper;

    public AgentToolRegistry(List<AgentTool> tools, ObjectMapper objectMapper) {
        this.tools = tools.stream().sorted(Comparator.comparing(AgentTool::name))
                .collect(Collectors.toMap(AgentTool::name, tool -> tool, (left, right) -> {
                    throw new IllegalStateException("重复的 Agent 工具名称：" + left.name());
                }, LinkedHashMap::new));
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> definitions(Authentication authentication) {
        return tools.values().stream().filter(tool -> isAllowed(tool, authentication)).map(tool -> Map.of(
                "type", "function",
                "function", Map.of("name", tool.name(), "description", tool.description(), "parameters", tool.parameterSchema())
        )).toList();
    }

    public AgentToolExecution execute(String toolName, String rawArguments, Authentication authentication, AgentToolContext context) {
        long startedAt = System.currentTimeMillis();
        AgentTool tool = tools.get(toolName);
        String argumentsSummary = trim(rawArguments, 1000);
        try {
            if (tool == null) throw BusinessException.badRequest("请求的工具未注册");
            if (!isAllowed(tool, authentication)) throw BusinessException.badRequest("当前用户无权调用该工具");
            JsonNode arguments = objectMapper.readTree(rawArguments == null ? "{}" : rawArguments);
            AgentToolArguments.validateAllowedFields(arguments, tool.allowedArguments());
            AgentToolOutput output = tool.execute(arguments, context);
            long duration = elapsed(startedAt);
            Map<String, Object> modelResult = Map.of("ok", true, "data", output.data(), "summary", output.summary());
            return new AgentToolExecution(toolName, argumentsSummary, modelResult,
                    new AiToolCallVO(toolName, output.summary(), true, duration), output.pendingAction(), null);
        } catch (BusinessException | JsonProcessingException exception) {
            return failed(toolName, argumentsSummary, startedAt, safeError(exception));
        } catch (RuntimeException exception) {
            return failed(toolName, argumentsSummary, startedAt, "工具执行暂时失败，请更换查询条件后重试");
        }
    }

    public String serializeForModel(AgentToolExecution execution) {
        try { return objectMapper.writeValueAsString(execution.modelResult()); }
        catch (JsonProcessingException exception) { return "{\"ok\":false,\"error\":\"工具结果序列化失败\"}"; }
    }

    private AgentToolExecution failed(String toolName, String argumentsSummary, long startedAt, String error) {
        long duration = elapsed(startedAt);
        return new AgentToolExecution(toolName, argumentsSummary, Map.of("ok", false, "error", error),
                new AiToolCallVO(toolName, error, false, duration), null, error);
    }
    private boolean isAllowed(AgentTool tool, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        boolean readOnly = authentication.getAuthorities().stream()
                .anyMatch(authority -> READ_ONLY_AUTHORITY.equals(authority.getAuthority()));
        if (readOnly && tool.sideEffect() == ToolSideEffect.WRITE) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> tool.requiredPermission().equals(authority.getAuthority()));
    }
    private long elapsed(long startedAt) { return Math.max(0, System.currentTimeMillis() - startedAt); }
    private String trim(String value, int max) {
        if (value == null) return "{}";
        return value.substring(0, Math.min(value.length(), max));
    }
    private String safeError(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "工具参数不合法" : message.substring(0, Math.min(message.length(), 300));
    }
}
