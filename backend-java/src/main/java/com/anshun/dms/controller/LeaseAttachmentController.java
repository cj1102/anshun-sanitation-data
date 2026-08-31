package com.anshun.dms.controller;

import com.anshun.dms.audit.OperationLog;
import com.anshun.dms.common.ApiResponse;
import com.anshun.dms.service.LeaseAttachmentService;
import com.anshun.dms.vo.LeaseAttachmentVO;
import jakarta.validation.constraints.Min;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/leases/{leaseId}/attachments")
public class LeaseAttachmentController {
    private final LeaseAttachmentService attachmentService;
    public LeaseAttachmentController(LeaseAttachmentService attachmentService) { this.attachmentService = attachmentService; }

    @GetMapping
    @PreAuthorize("hasAuthority('lease:view')")
    public ApiResponse<List<LeaseAttachmentVO>> list(@PathVariable @Min(1) long leaseId) {
        return ApiResponse.success(attachmentService.list(leaseId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('lease:update')")
    @OperationLog(module = "合同附件", action = "上传", target = "#leaseId")
    public ApiResponse<Void> upload(@PathVariable @Min(1) long leaseId, @RequestParam("file") MultipartFile file,
                                    Authentication authentication) {
        attachmentService.upload(leaseId, file, authentication == null ? "anonymous" : authentication.getName());
        return ApiResponse.successMessage("附件上传成功");
    }

    @GetMapping("/{attachmentId}/download")
    @PreAuthorize("hasAuthority('lease:view')")
    public ResponseEntity<InputStreamResource> download(@PathVariable @Min(1) long leaseId, @PathVariable @Min(1) long attachmentId) {
        LeaseAttachmentService.DownloadedAttachment attachment = attachmentService.download(leaseId, attachmentId);
        MediaType mediaType;
        try { mediaType = MediaType.parseMediaType(attachment.contentType()); }
        catch (IllegalArgumentException exception) { mediaType = MediaType.APPLICATION_OCTET_STREAM; }
        return ResponseEntity.ok().contentType(mediaType).contentLength(attachment.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(attachment.filename(), StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(attachment.stream()));
    }

    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("hasAuthority('lease:update')")
    @OperationLog(module = "合同附件", action = "删除", target = "#attachmentId")
    public ApiResponse<Void> delete(@PathVariable @Min(1) long leaseId, @PathVariable @Min(1) long attachmentId) {
        attachmentService.delete(leaseId, attachmentId);
        return ApiResponse.successMessage("附件删除成功");
    }
}
