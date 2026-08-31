package com.anshun.dms.vo;

import java.time.LocalDateTime;

/** Own-user Agent execution trace. It intentionally excludes raw prompts and full tool outputs. */
public record AiAgentRunVO(Long runId, Long conversationId, String model, String pageContext, String requestId,
                           String status, Integer toolCallCount, Long durationMs, String errorMessage,
                           LocalDateTime createTime, LocalDateTime completeTime) { }
