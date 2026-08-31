package com.anshun.dms.controller;

import com.anshun.dms.audit.OperationLog;
import com.anshun.dms.common.ApiResponse;
import com.anshun.dms.dto.AiAgentEvaluationCaseRequest;
import com.anshun.dms.service.AiAgentEvaluationService;
import com.anshun.dms.vo.AiAgentEvaluationCaseVO;
import com.anshun.dms.vo.AiAgentEvaluationOverviewVO;
import com.anshun.dms.vo.AiAgentEvaluationResultVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin-only quality dashboard and deterministic regression runner for the business Agent. */
@RestController
@RequestMapping("/api/ai/evaluation")
@PreAuthorize("hasAuthority('ai:evaluation:view')")
public class AiAgentEvaluationController {
    private final AiAgentEvaluationService evaluationService;
    public AiAgentEvaluationController(AiAgentEvaluationService evaluationService) { this.evaluationService = evaluationService; }

    @GetMapping("/overview")
    public ApiResponse<AiAgentEvaluationOverviewVO> overview(
            @RequestParam(defaultValue = "7") @Min(1) @Max(90) int days) {
        return ApiResponse.success(evaluationService.overview(days));
    }
    @GetMapping("/cases")
    public ApiResponse<List<AiAgentEvaluationCaseVO>> cases() { return ApiResponse.success(evaluationService.cases()); }
    @GetMapping("/results")
    public ApiResponse<List<AiAgentEvaluationResultVO>> results(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) { return ApiResponse.success(evaluationService.results(limit)); }

    @PostMapping("/cases")
    @PreAuthorize("hasAuthority('ai:evaluation:manage')")
    @OperationLog(module = "AI 评测", action = "创建评测用例", target = "#request.caseName")
    public ApiResponse<AiAgentEvaluationCaseVO> create(@Valid @RequestBody AiAgentEvaluationCaseRequest request,
                                                        Authentication authentication) {
        return ApiResponse.success(evaluationService.create(request, authentication.getName()));
    }
    @PostMapping("/cases/{caseId}/run")
    @PreAuthorize("hasAuthority('ai:evaluation:manage')")
    @OperationLog(module = "AI 评测", action = "运行评测用例", target = "#caseId")
    public ApiResponse<AiAgentEvaluationResultVO> run(@PathVariable @Min(1) long caseId, Authentication authentication) {
        return ApiResponse.success(evaluationService.run(caseId, authentication));
    }
}
