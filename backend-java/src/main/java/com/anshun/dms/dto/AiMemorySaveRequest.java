package com.anshun.dms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Content is deliberately user supplied; automatic capture requires an explicit chat command. */
public record AiMemorySaveRequest(
        @NotBlank(message = "记忆内容不能为空")
        @Size(max = 300, message = "单条记忆不能超过 300 个字符") String content,
        @Size(max = 32, message = "记忆类型不合法") String memoryType) { }
