package com.anshun.dms.agent;

import com.anshun.dms.vo.AiToolCallVO;
import com.anshun.dms.vo.AiPendingActionVO;

/** Outcome is returned to the model as structured data and to the browser as a safe summary. */
public record AgentToolExecution(String toolName, String argumentsSummary, Object modelResult,
                                 AiToolCallVO display, AiPendingActionVO pendingAction, String errorMessage) { }
