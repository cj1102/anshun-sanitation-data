package com.anshun.dms.vo;

import java.util.List;

public record AiChatResponse(String answer, String model, Long conversationId, boolean memorySaved, String memoryMessage,
                             Long agentRunId, List<AiToolCallVO> toolCalls, List<AiPendingActionVO> pendingActions) {
    /** Kept for existing callers while conversation persistence is introduced. */
    public AiChatResponse(String answer, String model) { this(answer, model, null, false, null, null, List.of(), List.of()); }
}
