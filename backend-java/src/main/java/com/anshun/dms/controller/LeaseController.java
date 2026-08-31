package com.anshun.dms.controller;

import com.anshun.dms.audit.OperationLog;
import com.anshun.dms.common.ApiResponse;
import com.anshun.dms.dto.LeasePageQuery;
import com.anshun.dms.dto.LeaseApprovalRequest;
import com.anshun.dms.dto.LeaseArchiveRequest;
import com.anshun.dms.dto.LeaseSaveRequest;
import com.anshun.dms.service.LeaseService;
import com.anshun.dms.vo.LeaseVO;
import com.anshun.dms.vo.PageData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/leases")
public class LeaseController {
    private final LeaseService leaseService;
    public LeaseController(LeaseService leaseService) { this.leaseService = leaseService; }

    @GetMapping
    @PreAuthorize("hasAuthority('lease:view')")
    public ApiResponse<PageData<LeaseVO>> list(@Valid LeasePageQuery query) {
        return ApiResponse.success(leaseService.page(query));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('lease:create')")
    @OperationLog(module = "租赁合同", action = "新增", target = "#request.contractCode")
    public ApiResponse<LeaseVO> create(@Valid @RequestBody LeaseSaveRequest request) {
        return ApiResponse.success(leaseService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('lease:update')")
    @OperationLog(module = "租赁合同", action = "更新", target = "#id")
    public ApiResponse<LeaseVO> update(@PathVariable @Min(1) long id, @Valid @RequestBody LeaseSaveRequest request) {
        return ApiResponse.success(leaseService.update(id, request));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('lease:submit')")
    @OperationLog(module = "合同审批", action = "提交审核", target = "#id")
    public ApiResponse<LeaseVO> submit(@PathVariable @Min(1) long id, Authentication authentication) {
        return ApiResponse.success(leaseService.submitForApproval(id, authentication.getName()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('lease:approve')")
    @OperationLog(module = "合同审批", action = "审核通过", target = "#id")
    public ApiResponse<LeaseVO> approve(@PathVariable @Min(1) long id, @Valid @RequestBody LeaseApprovalRequest request,
                                         Authentication authentication) {
        return ApiResponse.success(leaseService.approve(id, request, authentication.getName()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('lease:approve')")
    @OperationLog(module = "合同审批", action = "审核驳回", target = "#id")
    public ApiResponse<LeaseVO> reject(@PathVariable @Min(1) long id, @Valid @RequestBody LeaseApprovalRequest request,
                                        Authentication authentication) {
        return ApiResponse.success(leaseService.reject(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('lease:delete')")
    @OperationLog(module = "租赁合同", action = "归档", target = "#id")
    public ApiResponse<Void> archive(@PathVariable @Min(1) long id,
                                     @Valid @RequestBody LeaseArchiveRequest request) {
        leaseService.archive(id, request.version());
        return ApiResponse.successMessage("合同已归档");
    }
}
