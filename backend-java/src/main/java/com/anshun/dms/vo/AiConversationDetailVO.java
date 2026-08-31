package com.anshun.dms.vo;

import java.util.List;

public record AiConversationDetailVO(Long conversationId, String title, List<AiChatMessageVO> messages) { }
