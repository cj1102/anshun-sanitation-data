package com.anshun.dms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AiChatRequest(
        @NotBlank(message = "请输入要咨询的问题") @Size(max = 2000, message = "单次提问不能超过 2000 个字符") String message,
        @Positive(message = "对话编号必须大于 0") Long conversationId,
        @Size(max = 100, message = "页面上下文过长") String page) { }
