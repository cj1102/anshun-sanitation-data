package com.anshun.dms.vo;

import java.time.LocalDateTime;

/** One conversation visible only to its owner in the AI history drawer. */
public record AiConversationSummaryVO(Long conversationId, String title, String preview, LocalDateTime lastMessageAt) { }
