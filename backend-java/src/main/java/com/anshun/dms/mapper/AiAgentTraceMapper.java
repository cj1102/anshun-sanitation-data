package com.anshun.dms.mapper;

import com.anshun.dms.vo.AiAgentRunVO;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiAgentTraceMapper {
    Integer selectUserId(@Param("username") String username);
    long countRuns(@Param("userId") int userId);
    List<AiAgentRunVO> selectRuns(@Param("userId") int userId, @Param("limit") int limit, @Param("offset") int offset);

    @Options(useGeneratedKeys = true, keyProperty = "runId", keyColumn = "run_id")
    int insertRun(AgentRunDraft draft);
    int completeRun(@Param("runId") long runId, @Param("conversationId") Long conversationId,
                    @Param("status") String status, @Param("toolCallCount") int toolCallCount,
                    @Param("durationMs") long durationMs, @Param("errorMessage") String errorMessage);
    int insertToolCall(AgentToolCallDraft draft);

    final class AgentRunDraft {
        private Long runId;
        private final Integer userId;
        private final String model;
        private final String pageContext;
        private final String requestId;
        private final String runType;

        public AgentRunDraft(Integer userId, String model, String pageContext, String requestId, String runType) {
            this.userId = userId; this.model = model; this.pageContext = pageContext; this.requestId = requestId;
            this.runType = runType;
        }
        public Long getRunId() { return runId; }
        public void setRunId(Long runId) { this.runId = runId; }
        public Integer getUserId() { return userId; }
        public String getModel() { return model; }
        public String getPageContext() { return pageContext; }
        public String getRequestId() { return requestId; }
        public String getRunType() { return runType; }
    }

    public record AgentToolCallDraft(Long runId, Integer sequenceNo, String providerCallId, String toolName,
                                     String argumentsSummary, String resultSummary, boolean success,
                                     Long durationMs, String errorMessage) { }
}
