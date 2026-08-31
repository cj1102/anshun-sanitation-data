package com.anshun.dms.vo;

import java.time.LocalDateTime;

public record AiAgentEvaluationCaseVO(Long caseId, String caseName, String question, String pageContext,
                                      String expectedToolName, String expectedKeywords, Boolean enabled,
                                      String creatorUsername, LocalDateTime createTime) {
}
