package com.anshun.dms.agent;

/** Trusted request metadata that the model cannot provide or override. */
public record AgentToolContext(String username, long agentRunId, String pageContext) { }
