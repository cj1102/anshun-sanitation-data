package com.anshun.dms.service;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.mapper.AiConversationMapper;
import com.anshun.dms.vo.AiChatMessageVO;
import com.anshun.dms.vo.AiConversationDetailVO;
import com.anshun.dms.vo.AiConversationSummaryVO;
import com.anshun.dms.vo.PageData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Owns conversation persistence and never stores system prompts or API credentials. */
@Service
public class AiConversationService {
    private static final int MODEL_HISTORY_LIMIT = 10;
    private final AiConversationMapper mapper;

    public AiConversationService(AiConversationMapper mapper) { this.mapper = mapper; }

    public ConversationContext prepare(String username, Long conversationId) {
        int userId = requireUserId(username);
        if (conversationId == null) return new ConversationContext(userId, null, List.of());
        AiConversationMapper.ConversationOwner owner = requireConversation(conversationId, userId);
        return new ConversationContext(userId, owner.conversationId(), mapper.selectRecentMessages(conversationId, MODEL_HISTORY_LIMIT));
    }

    /** Stores a complete successful exchange after the model response, avoiding half-finished conversations. */
    @Transactional
    public long saveExchange(ConversationContext context, String question, String page, String answer, String model) {
        long conversationId = context.conversationId() == null ? createConversation(context.userId(), question) : context.conversationId();
        mapper.insertMessage(new AiConversationMapper.MessageDraft(conversationId, "user", question, null, page));
        mapper.insertMessage(new AiConversationMapper.MessageDraft(conversationId, "assistant", answer, model, null));
        mapper.touchConversation(conversationId);
        return conversationId;
    }

    public PageData<AiConversationSummaryVO> list(String username, int page, int pageSize) {
        int userId = requireUserId(username);
        int safePage = Math.max(1, page);
        int safePageSize = Math.min(50, Math.max(1, pageSize));
        return new PageData<>(mapper.countConversations(userId), mapper.selectConversations(userId, safePageSize, (safePage - 1) * safePageSize));
    }

    public AiConversationDetailVO detail(String username, long conversationId) {
        int userId = requireUserId(username);
        AiConversationMapper.ConversationOwner owner = requireConversation(conversationId, userId);
        List<AiChatMessageVO> messages = mapper.selectMessages(conversationId);
        return new AiConversationDetailVO(owner.conversationId(), owner.title(), messages);
    }

    @Transactional
    public void delete(String username, long conversationId) {
        int userId = requireUserId(username);
        if (mapper.logicalDelete(conversationId, userId) == 0) throw BusinessException.notFound("对话不存在或无权访问");
    }

    private long createConversation(int userId, String firstQuestion) {
        AiConversationMapper.ConversationDraft draft = new AiConversationMapper.ConversationDraft(userId, createTitle(firstQuestion));
        mapper.insertConversation(draft);
        if (draft.getConversationId() == null) throw BusinessException.unavailable("创建对话失败，请稍后重试");
        return draft.getConversationId();
    }

    private int requireUserId(String username) {
        Integer userId = mapper.selectUserId(username);
        if (userId == null) throw BusinessException.notFound("当前用户不存在或已禁用");
        return userId;
    }

    private AiConversationMapper.ConversationOwner requireConversation(long conversationId, int userId) {
        AiConversationMapper.ConversationOwner owner = mapper.selectConversationForUser(conversationId, userId);
        if (owner == null) throw BusinessException.notFound("对话不存在或无权访问");
        return owner;
    }

    private String createTitle(String question) {
        String normalized = question == null ? "新对话" : question.replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) return "新对话";
        return normalized.length() <= 36 ? normalized : normalized.substring(0, 36) + "…";
    }

    public record ConversationContext(int userId, Long conversationId, List<AiConversationMapper.MessageContext> history) { }
}
