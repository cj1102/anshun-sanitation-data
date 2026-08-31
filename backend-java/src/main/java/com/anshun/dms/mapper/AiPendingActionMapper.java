package com.anshun.dms.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AiPendingActionMapper {
    Integer selectUserId(@Param("username") String username);
    ActionRecord selectForUser(@Param("userId") int userId, @Param("actionId") String actionId);
    List<ActionRecord> selectPendingForUser(@Param("userId") int userId);
    int insert(ActionDraft draft);
    int claimForExecution(@Param("userId") int userId, @Param("actionId") String actionId);
    int markExecuted(@Param("userId") int userId, @Param("actionId") String actionId, @Param("resultJson") String resultJson);
    int markFailed(@Param("userId") int userId, @Param("actionId") String actionId, @Param("errorMessage") String errorMessage);
    int markExpired(@Param("userId") int userId, @Param("actionId") String actionId);
    int cancel(@Param("userId") int userId, @Param("actionId") String actionId);
    int cancelPendingForRun(@Param("agentRunId") long agentRunId, @Param("errorMessage") String errorMessage);
    int bindConversation(@Param("actionIds") List<String> actionIds, @Param("conversationId") long conversationId);

    record ActionRecord(String actionId, Integer userId, Long agentRunId, Long conversationId, String actionType,
                        String requiredPermission, String status, String payloadJson, String payloadHash,
                        String resultJson, String summary, String pageContext, String requestId,
                        LocalDateTime expiresAt, LocalDateTime confirmedAt, LocalDateTime executedAt,
                        String errorMessage, LocalDateTime createTime) { }

    final class ActionDraft {
        private final String actionId;
        private final Integer userId;
        private final Long agentRunId;
        private final String actionType;
        private final String requiredPermission;
        private final String payloadJson;
        private final String payloadHash;
        private final String summary;
        private final String pageContext;
        private final String requestId;
        private final LocalDateTime expiresAt;

        public ActionDraft(String actionId, Integer userId, Long agentRunId, String actionType, String requiredPermission,
                           String payloadJson, String payloadHash, String summary, String pageContext,
                           String requestId, LocalDateTime expiresAt) {
            this.actionId = actionId;
            this.userId = userId;
            this.agentRunId = agentRunId;
            this.actionType = actionType;
            this.requiredPermission = requiredPermission;
            this.payloadJson = payloadJson;
            this.payloadHash = payloadHash;
            this.summary = summary;
            this.pageContext = pageContext;
            this.requestId = requestId;
            this.expiresAt = expiresAt;
        }
        public String getActionId() { return actionId; }
        public Integer getUserId() { return userId; }
        public Long getAgentRunId() { return agentRunId; }
        public String getActionType() { return actionType; }
        public String getRequiredPermission() { return requiredPermission; }
        public String getPayloadJson() { return payloadJson; }
        public String getPayloadHash() { return payloadHash; }
        public String getSummary() { return summary; }
        public String getPageContext() { return pageContext; }
        public String getRequestId() { return requestId; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
    }
}
