package com.anshun.dms.controller;

import com.anshun.dms.audit.OperationLog;
import com.anshun.dms.common.ApiResponse;
import com.anshun.dms.dto.AiMemorySaveRequest;
import com.anshun.dms.service.AiUserMemoryService;
import com.anshun.dms.vo.AiUserMemoryVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Every endpoint is scoped to Authentication#getName; no user id is accepted from the browser. */
@RestController
@RequestMapping("/api/ai/memories")
@PreAuthorize("isAuthenticated()")
public class AiMemoryController {
    private final AiUserMemoryService memoryService;

    public AiMemoryController(AiUserMemoryService memoryService) { this.memoryService = memoryService; }

    @GetMapping
    public ApiResponse<List<AiUserMemoryVO>> list(Authentication authentication) {
        return ApiResponse.success(memoryService.list(authentication.getName()));
    }

    @PostMapping
    @OperationLog(module = "AI 长期记忆", action = "新增记忆", target = "#request.memoryType")
    public ApiResponse<AiUserMemoryVO> save(@Valid @RequestBody AiMemorySaveRequest request, Authentication authentication) {
        return ApiResponse.success(memoryService.saveManual(authentication.getName(), request.content(), request.memoryType()));
    }

    @DeleteMapping("/{memoryId}")
    @OperationLog(module = "AI 长期记忆", action = "删除记忆", target = "#memoryId")
    public ApiResponse<Void> delete(@PathVariable @Min(value = 1, message = "记忆编号必须大于 0") long memoryId,
                                    Authentication authentication) {
        memoryService.delete(authentication.getName(), memoryId);
        return ApiResponse.successMessage("长期记忆已删除");
    }
}
