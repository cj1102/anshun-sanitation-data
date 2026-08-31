package com.anshun.dms.agent;

import com.anshun.dms.vo.AiPendingActionVO;

/** Tool data passed back to the model plus a small, auditable summary. */
public record AgentToolOutput(Object data, String summary, AiPendingActionVO pendingAction) {
    public AgentToolOutput(Object data, String summary) { this(data, summary, null); }
}
