package com.anshun.dms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A deterministic expectation for a manually triggered Agent regression check. */
public record AiAgentEvaluationCaseRequest(
        @NotBlank(message = "评测名称不能为空") @Size(max = 120, message = "评测名称不能超过 120 个字符") String caseName,
        @NotBlank(message = "评测问题不能为空") @Size(max = 2000, message = "评测问题不能超过 2000 个字符") String question,
        @Size(max = 100, message = "页面上下文不能超过 100 个字符") String pageContext,
        @Size(max = 64, message = "期望工具名不能超过 64 个字符") String expectedToolName,
        @Size(max = 500, message = "期望关键词不能超过 500 个字符") String expectedKeywords) {
}
