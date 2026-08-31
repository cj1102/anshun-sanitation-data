package com.anshun.dms.vo;

import java.time.LocalDateTime;

/** Persisted user or assistant message. System prompts are intentionally never stored here. */
public record AiChatMessageVO(Long messageId, String role, String content, String model, LocalDateTime createTime) { }
