package com.anshun.dms.service;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.dto.AiAgentEvaluationCaseRequest;
import com.anshun.dms.dto.AiChatRequest;
import com.anshun.dms.mapper.AiAgentEvaluationMapper;
import com.anshun.dms.vo.AiAgentEvaluationCaseVO;
import com.anshun.dms.vo.AiAgentEvaluationOverviewVO;
import com.anshun.dms.vo.AiAgentEvaluationResultVO;
import com.anshun.dms.vo.AiChatResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.anshun.dms.agent.AgentToolRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Combines online outcome metrics with small, repeatable offline regression cases.
 * Evaluation calls deliberately remove write permissions, so a benchmark can never generate a pending write action.
 */
@Service
public class AiAgentEvaluationService {
    private final AiAgentEvaluationMapper mapper;
    private final DeepSeekAssistantService assistantService;

    public AiAgentEvaluationService(AiAgentEvaluationMapper mapper, DeepSeekAssistantService assistantService) {
        this.mapper = mapper;
        this.assistantService = assistantService;
    }

    public AiAgentEvaluationOverviewVO overview(int days) {
        int safeDays = Math.max(1, Math.min(days, 90));
        AiAgentEvaluationMapper.EvaluationMetrics runs = mapper.selectRunMetrics(safeDays);
        AiAgentEvaluationMapper.ToolMetrics tools = mapper.selectToolMetrics(safeDays);
        AiAgentEvaluationMapper.FeedbackMetrics feedback = mapper.selectFeedbackMetrics(safeDays);
        return new AiAgentEvaluationOverviewVO(runs.totalRuns(), runs.succeededRuns(), runs.failedRuns(),
                runs.averageDurationMs(), tools.totalToolCalls(), tools.failedToolCalls(), feedback.feedbackTotal(),
                feedback.positiveFeedback(), feedback.negativeFeedback(), rate(runs.succeededRuns(), runs.totalRuns()),
                rate(tools.totalToolCalls() - tools.failedToolCalls(), tools.totalToolCalls()),
                rate(feedback.positiveFeedback(), feedback.feedbackTotal()));
    }

    public List<AiAgentEvaluationCaseVO> cases() { return mapper.selectCases(); }
    public List<AiAgentEvaluationResultVO> results(int limit) { return mapper.selectResults(Math.max(1, Math.min(limit, 100))); }

    @Transactional
    public AiAgentEvaluationCaseVO create(AiAgentEvaluationCaseRequest request, String username) {
        String expectedTool = blankToNull(request.expectedToolName());
        String expectedKeywords = blankToNull(request.expectedKeywords());
        if (expectedTool == null && expectedKeywords == null) {
            throw BusinessException.badRequest("至少填写期望工具名或期望关键词之一");
        }
        Integer userId = mapper.selectActiveUserId(username);
        if (userId == null) throw BusinessException.notFound("当前用户不存在或已禁用");
        AiAgentEvaluationMapper.EvaluationCaseDraft draft = new AiAgentEvaluationMapper.EvaluationCaseDraft(
                trim(request.caseName(), 120), trim(request.question(), 2000),
                StringUtils.hasText(request.pageContext()) ? trim(request.pageContext(), 100) : "/dashboard",
                expectedTool, expectedKeywords, userId, username);
        mapper.insertCase(draft);
        if (draft.getCaseId() == null) throw BusinessException.unavailable("评测用例保存失败，请稍后重试");
        return mapper.selectCase(draft.getCaseId());
    }

    /** Runs one real model call and scores only deterministic expectations: expected tool and required answer keywords. */
    public AiAgentEvaluationResultVO run(long caseId, Authentication authentication) {
        AiAgentEvaluationCaseVO evaluationCase = mapper.selectCase(caseId);
        if (evaluationCase == null || !Boolean.TRUE.equals(evaluationCase.enabled())) throw BusinessException.notFound("评测用例不存在或已停用");
        if (authentication == null || !authentication.isAuthenticated()) throw BusinessException.notFound("当前用户不存在或已禁用");
        long startedAt = System.currentTimeMillis();
        try {
            AiChatResponse response = assistantService.chatForEvaluation(new AiChatRequest(evaluationCase.question(), null, evaluationCase.pageContext()),
                    readOnlyAuthentication(authentication));
            String actualTools = response.toolCalls().stream().map(item -> item.toolName()).distinct().sorted().collect(Collectors.joining(","));
            boolean toolMatched = !StringUtils.hasText(evaluationCase.expectedToolName()) || actualTools.contains(evaluationCase.expectedToolName());
            Set<String> keywords = expectedKeywords(evaluationCase.expectedKeywords());
            String normalizedAnswer = normalize(response.answer());
            boolean keywordsMatched = keywords.isEmpty() || keywords.stream().allMatch(normalizedAnswer::contains);
            boolean passed = toolMatched && keywordsMatched;
            String detail = detail(evaluationCase.expectedToolName(), actualTools, keywords, normalizedAnswer, toolMatched, keywordsMatched);
            return saveResult(evaluationCase, response.agentRunId(), response.model(), actualTools, toolMatched, keywordsMatched,
                    passed, detail, elapsed(startedAt), authentication.getName());
        } catch (BusinessException exception) {
            return saveResult(evaluationCase, null, null, "", false, false, false,
                    "Agent 调用失败：" + trim(exception.getMessage(), 500), elapsed(startedAt), authentication.getName());
        }
    }

    private AiAgentEvaluationResultVO saveResult(AiAgentEvaluationCaseVO evaluationCase, Long agentRunId, String model,
                                                  String actualTools, boolean toolMatched, boolean keywordsMatched,
                                                  boolean passed, String detail, long durationMs, String evaluatorUsername) {
        AiAgentEvaluationMapper.EvaluationResultDraft draft = new AiAgentEvaluationMapper.EvaluationResultDraft(
                evaluationCase.caseId(), agentRunId, model, blankToNull(actualTools), toolMatched, keywordsMatched,
                passed, trim(detail, 1000), durationMs, evaluatorUsername);
        mapper.insertResult(draft);
        return new AiAgentEvaluationResultVO(draft.getResultId(), evaluationCase.caseId(), evaluationCase.caseName(),
                agentRunId, model, blankToNull(actualTools), toolMatched, keywordsMatched, passed, trim(detail, 1000),
                durationMs, evaluatorUsername, null);
    }

    private Authentication readOnlyAuthentication(Authentication source) {
        List<GrantedAuthority> authorities = new java.util.ArrayList<>(source.getAuthorities());
        authorities.add(new SimpleGrantedAuthority(AgentToolRegistry.READ_ONLY_AUTHORITY));
        return new UsernamePasswordAuthenticationToken(source.getPrincipal(), "", authorities);
    }

    private Set<String> expectedKeywords(String value) {
        if (!StringUtils.hasText(value)) return Set.of();
        return Arrays.stream(value.split("[,，|、]"))
                .map(this::normalize).filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    private String detail(String expectedTool, String actualTools, Set<String> keywords, String answer,
                          boolean toolMatched, boolean keywordsMatched) {
        StringBuilder result = new StringBuilder();
        if (StringUtils.hasText(expectedTool)) result.append(toolMatched ? "工具命中：" : "缺少期望工具：").append(expectedTool)
                .append("；实际工具：").append(StringUtils.hasText(actualTools) ? actualTools : "无");
        if (!keywords.isEmpty()) {
            if (result.length() > 0) result.append("；");
            result.append(keywordsMatched ? "关键词全部命中：" : "关键词未完全命中：").append(String.join("、", keywords));
        }
        return result.length() == 0 ? "没有可评分的期望条件" : result.toString();
    }
    private double rate(long numerator, long denominator) { return denominator == 0 ? 0 : Math.round(numerator * 10_000D / denominator) / 100D; }
    private long elapsed(long startedAt) { return Math.max(0, System.currentTimeMillis() - startedAt); }
    private String blankToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
    private String trim(String value, int max) { String safe = value == null ? "" : value.trim(); return safe.substring(0, Math.min(safe.length(), max)); }
    private String normalize(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", ""); }
}
