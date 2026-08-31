package com.anshun.dms.vo;

import java.time.LocalDateTime;

/** Stores pass/fail evidence, but intentionally never stores the raw model answer. */
public record AiAgentEvaluationResultVO(Long resultId, Long caseId, String caseName, Long agentRunId, String model,
                                        String actualTools, Boolean expectedToolMatched, Boolean expectedKeywordsMatched,
                                        Boolean passed, String detail, Long durationMs, String evaluatorUsername,
                                        LocalDateTime createTime) {
}
