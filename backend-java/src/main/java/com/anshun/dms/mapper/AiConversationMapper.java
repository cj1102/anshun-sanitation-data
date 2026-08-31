package com.anshun.dms.mapper;

import com.anshun.dms.vo.AiChatMessageVO;
import com.anshun.dms.vo.AiConversationSummaryVO;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiConversationMapper {
    Integer selectUserId(@Param("username") String username);
    ConversationOwner selectConversationForUser(@Param("conversationId") long conversationId, @Param("userId") int userId);
    long countConversations(@Param("userId") int userId);
    List<AiConversationSummaryVO> selectConversations(@Param("userId") int userId, @Param("limit") int limit, @Param("offset") int offset);
    List<AiChatMessageVO> selectMessages(@Param("conversationId") long conversationId);
    List<MessageContext> selectRecentMessages(@Param("conversationId") long conversationId, @Param("limit") int limit);

    @Options(useGeneratedKeys = true, keyProperty = "conversationId", keyColumn = "conversation_id")
    int insertConversation(ConversationDraft draft);
    int insertMessage(MessageDraft draft);
    int touchConversation(@Param("conversationId") long conversationId);
    int logicalDelete(@Param("conversationId") long conversationId, @Param("userId") int userId);

    record ConversationOwner(Long conversationId, Integer userId, String title) { }
    record MessageContext(String role, String content) { }
    record MessageDraft(Long conversationId, String role, String content, String model, String pageContext) { }

    final class ConversationDraft {
        private Long conversationId;
        private final Integer userId;
        private final String title;

        public ConversationDraft(Integer userId, String title) {
            this.userId = userId;
            this.title = title;
        }
        public Long getConversationId() { return conversationId; }
        public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
        public Integer getUserId() { return userId; }
        public String getTitle() { return title; }
    }
}
