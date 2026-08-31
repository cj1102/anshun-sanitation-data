package com.anshun.dms.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

/** Explicit user feedback for one Agent run; it is never inferred from private conversation content. */
public record AiAgentFeedbackRequest(
        @NotBlank(message = "请给出评分")
        @Pattern(regexp = "UP|DOWN", message = "评分只能是 UP 或 DOWN") String rating,
        @Size(max = 500, message = "反馈说明不能超过 500 个字符") String comment) {
}
