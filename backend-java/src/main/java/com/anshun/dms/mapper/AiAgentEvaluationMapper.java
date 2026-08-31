package com.anshun.dms.mapper;

import com.anshun.dms.vo.AiAgentEvaluationCaseVO;
import com.anshun.dms.vo.AiAgentEvaluationResultVO;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiAgentEvaluationMapper {
    Integer selectActiveUserId(@Param("username") String username);
    EvaluationMetrics selectRunMetrics(@Param("days") int days);
    ToolMetrics selectToolMetrics(@Param("days") int days);
    FeedbackMetrics selectFeedbackMetrics(@Param("days") int days);
    List<AiAgentEvaluationCaseVO> selectCases();
    AiAgentEvaluationCaseVO selectCase(@Param("caseId") long caseId);
    List<AiAgentEvaluationResultVO> selectResults(@Param("limit") int limit);

    @Options(useGeneratedKeys = true, keyProperty = "caseId", keyColumn = "case_id")
    int insertCase(EvaluationCaseDraft draft);
    @Options(useGeneratedKeys = true, keyProperty = "resultId", keyColumn = "result_id")
    int insertResult(EvaluationResultDraft draft);

    record EvaluationMetrics(long totalRuns, long succeededRuns, long failedRuns, long averageDurationMs) { }
    record ToolMetrics(long totalToolCalls, long failedToolCalls) { }
    record FeedbackMetrics(long feedbackTotal, long positiveFeedback, long negativeFeedback) { }

    final class EvaluationCaseDraft {
        private Long caseId;
        private final String caseName, question, pageContext, expectedToolName, expectedKeywords, creatorUsername;
        private final Integer creatorId;
        public EvaluationCaseDraft(String caseName, String question, String pageContext, String expectedToolName,
                                   String expectedKeywords, Integer creatorId, String creatorUsername) {
            this.caseName = caseName; this.question = question; this.pageContext = pageContext;
            this.expectedToolName = expectedToolName; this.expectedKeywords = expectedKeywords;
            this.creatorId = creatorId; this.creatorUsername = creatorUsername;
        }
        public Long getCaseId() { return caseId; }
        public void setCaseId(Long caseId) { this.caseId = caseId; }
        public String getCaseName() { return caseName; }
        public String getQuestion() { return question; }
        public String getPageContext() { return pageContext; }
        public String getExpectedToolName() { return expectedToolName; }
        public String getExpectedKeywords() { return expectedKeywords; }
        public Integer getCreatorId() { return creatorId; }
        public String getCreatorUsername() { return creatorUsername; }
    }

    final class EvaluationResultDraft {
        private Long resultId;
        private final Long caseId, agentRunId, durationMs;
        private final String model, actualTools, detail, evaluatorUsername;
        private final boolean expectedToolMatched, expectedKeywordsMatched, passed;
        public EvaluationResultDraft(Long caseId, Long agentRunId, String model, String actualTools,
                                     boolean expectedToolMatched, boolean expectedKeywordsMatched, boolean passed,
                                     String detail, Long durationMs, String evaluatorUsername) {
            this.caseId = caseId; this.agentRunId = agentRunId; this.model = model; this.actualTools = actualTools;
            this.expectedToolMatched = expectedToolMatched; this.expectedKeywordsMatched = expectedKeywordsMatched;
            this.passed = passed; this.detail = detail; this.durationMs = durationMs; this.evaluatorUsername = evaluatorUsername;
        }
        public Long getResultId() { return resultId; }
        public void setResultId(Long resultId) { this.resultId = resultId; }
        public Long getCaseId() { return caseId; }
        public Long getAgentRunId() { return agentRunId; }
        public String getModel() { return model; }
        public String getActualTools() { return actualTools; }
        public boolean isExpectedToolMatched() { return expectedToolMatched; }
        public boolean isExpectedKeywordsMatched() { return expectedKeywordsMatched; }
        public boolean isPassed() { return passed; }
        public String getDetail() { return detail; }
        public Long getDurationMs() { return durationMs; }
        public String getEvaluatorUsername() { return evaluatorUsername; }
    }
}
