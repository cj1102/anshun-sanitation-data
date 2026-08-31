package com.anshun.dms.service;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.mapper.AiAgentTraceMapper;
import com.anshun.dms.vo.AiAgentRunVO;
import com.anshun.dms.vo.PageData;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Stores execution metadata and safe summaries, never raw prompts, tokens, or full model output. */
@Service
public class AiAgentTraceService {
    private final AiAgentTraceMapper mapper;

    public AiAgentTraceService(AiAgentTraceMapper mapper) { this.mapper = mapper; }

    @Transactional
    public AgentRun start(String username, String model, String pageContext) {
        return start(username, model, pageContext, "ONLINE");
    }

    @Transactional
    public AgentRun start(String username, String model, String pageContext, String runType) {
        int userId = requireUserId(username);
        String safeRunType = "EVALUATION".equals(runType) ? "EVALUATION" : "ONLINE";
        AiAgentTraceMapper.AgentRunDraft draft = new AiAgentTraceMapper.AgentRunDraft(
                userId, model, trim(pageContext, 100), MDC.get("requestId"), safeRunType);
        mapper.insertRun(draft);
        if (draft.getRunId() == null) throw BusinessException.unavailable("Agent 运行轨迹创建失败，请稍后重试");
        return new AgentRun(draft.getRunId(), System.currentTimeMillis());
    }

    public void recordToolCall(long runId, int sequenceNo, String providerCallId, String toolName,
                               String argumentsSummary, String resultSummary, boolean success,
                               long durationMs, String errorMessage) {
        mapper.insertToolCall(new AiAgentTraceMapper.AgentToolCallDraft(runId, sequenceNo, trim(providerCallId, 128),
                trim(toolName, 64), trim(argumentsSummary, 1000), trim(resultSummary, 4000), success,
                durationMs, trim(errorMessage, 500)));
    }

    public void succeed(AgentRun run, Long conversationId, int toolCallCount) {
        mapper.completeRun(run.runId(), conversationId, "SUCCEEDED", toolCallCount, elapsed(run), null);
    }

    public void fail(AgentRun run, int toolCallCount, String errorMessage) {
        mapper.completeRun(run.runId(), null, "FAILED", toolCallCount, elapsed(run), trim(errorMessage, 500));
    }

    public PageData<AiAgentRunVO> list(String username, int page, int pageSize) {
        int userId = requireUserId(username);
        int safePage = Math.max(1, page);
        int safePageSize = Math.min(50, Math.max(1, pageSize));
        return new PageData<>(mapper.countRuns(userId), mapper.selectRuns(userId, safePageSize, (safePage - 1) * safePageSize));
    }

    private int requireUserId(String username) {
        Integer userId = mapper.selectUserId(username);
        if (userId == null) throw BusinessException.notFound("当前用户不存在或已禁用");
        return userId;
    }
    private long elapsed(AgentRun run) { return Math.max(0, System.currentTimeMillis() - run.startedAt()); }
    private String trim(String value, int max) { return !StringUtils.hasText(value) ? null : value.substring(0, Math.min(value.length(), max)); }

    public record AgentRun(long runId, long startedAt) { }
}
