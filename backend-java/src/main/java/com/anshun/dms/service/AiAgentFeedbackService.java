package com.anshun.dms.service;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.dto.AiAgentFeedbackRequest;
import com.anshun.dms.mapper.AiAgentFeedbackMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Persists explicit, user-owned quality signals for later offline Agent evaluation. */
@Service
public class AiAgentFeedbackService {
    private final AiAgentFeedbackMapper mapper;

    public AiAgentFeedbackService(AiAgentFeedbackMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public void save(String username, long runId, AiAgentFeedbackRequest request) {
        Integer userId = mapper.selectActiveUserId(username);
        if (userId == null) throw BusinessException.notFound("当前用户不存在或已禁用");
        String comment = trim(request.comment());
        int changed = mapper.upsertForOwnedRun(runId, userId, request.rating(), comment);
        if (changed == 0) throw BusinessException.notFound("未找到本人的 AI 运行记录");
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
