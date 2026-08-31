package com.anshun.dms.vo;

/** Rolling online outcome metrics. Rates are percentages in the [0, 100] range. */
public record AiAgentEvaluationOverviewVO(long totalRuns, long succeededRuns, long failedRuns, long averageDurationMs,
                                          long totalToolCalls, long failedToolCalls, long feedbackTotal,
                                          long positiveFeedback, long negativeFeedback, double successRate,
                                          double toolSuccessRate, double positiveFeedbackRate) {
}
