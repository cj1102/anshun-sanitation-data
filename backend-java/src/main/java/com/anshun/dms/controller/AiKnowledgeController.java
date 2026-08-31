package com.anshun.dms.controller;

import com.anshun.dms.audit.OperationLog;
import com.anshun.dms.common.ApiResponse;
import com.anshun.dms.service.AiKnowledgeService;
import com.anshun.dms.vo.AiKnowledgeDocumentVO;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Knowledge administration is deliberately separate from normal AI chat access. */
@RestController
@RequestMapping("/api/ai/knowledge/documents")
@PreAuthorize("hasAuthority('ai:knowledge:manage')")
public class AiKnowledgeController {
    private final AiKnowledgeService knowledgeService;

    public AiKnowledgeController(AiKnowledgeService knowledgeService) { this.knowledgeService = knowledgeService; }

    @GetMapping
    public ApiResponse<List<AiKnowledgeDocumentVO>> list() { return ApiResponse.success(knowledgeService.listDocuments()); }

    @PostMapping
    @OperationLog(module = "AI 知识库", action = "上传文档", target = "#file.originalFilename")
    public ApiResponse<AiKnowledgeDocumentVO> upload(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(required = false) String title,
                                                      @RequestParam(defaultValue = "ALL") String visibleRoles,
                                                      Authentication authentication) {
        return ApiResponse.success(knowledgeService.upload(file, title, visibleRoles, authentication.getName()));
    }

    @PostMapping("/reindex")
    @OperationLog(module = "AI 知识库", action = "重建向量索引")
    public ApiResponse<AiKnowledgeService.ReindexResult> reindex() {
        return ApiResponse.success(knowledgeService.reindexVectors());
    }

    @DeleteMapping("/{documentId}")
    @OperationLog(module = "AI 知识库", action = "删除文档", target = "#documentId")
    public ApiResponse<Void> delete(@PathVariable @Min(value = 1, message = "文档编号必须大于 0") long documentId) {
        knowledgeService.delete(documentId);
        return ApiResponse.successMessage("知识库文档已删除");
    }
}
