package com.anshun.dms.agent;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Set;

/** A deliberately allow-listed business capability exposed to the model. */
public interface AgentTool {
    String name();
    String description();
    String requiredPermission();
    default ToolSideEffect sideEffect() { return ToolSideEffect.READ; }
    Set<String> allowedArguments();
    Map<String, Object> parameterSchema();
    AgentToolOutput execute(JsonNode arguments, AgentToolContext context);
}
