package com.anshun.dms.controller;

import com.anshun.dms.audit.OperationLog;
import com.anshun.dms.common.ApiResponse;
import com.anshun.dms.dto.PositionPageQuery;
import com.anshun.dms.dto.PositionSaveRequest;
import com.anshun.dms.service.PositionService;
import com.anshun.dms.vo.PageData;
import com.anshun.dms.vo.PositionDetailVO;
import com.anshun.dms.vo.PositionVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/positions")
public class PositionController {
    private final PositionService positionService;
    public PositionController(PositionService positionService) { this.positionService = positionService; }

    @GetMapping
    @PreAuthorize("hasAuthority('position:view')")
    public ApiResponse<PageData<PositionVO>> list(@Valid PositionPageQuery query) {
        return ApiResponse.success(positionService.page(query));
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasAuthority('position:view')")
    public ApiResponse<PositionDetailVO> detail(@PathVariable String code) {
        return ApiResponse.success(positionService.detail(code));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('position:create')")
    @OperationLog(module = "广告点位", action = "新增", target = "#request.adPositionCode")
    public ApiResponse<PositionVO> create(@Valid @RequestBody PositionSaveRequest request) {
        return ApiResponse.success(positionService.create(request));
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasAuthority('position:update')")
    @OperationLog(module = "广告点位", action = "更新", target = "#code")
    public ApiResponse<PositionVO> update(@PathVariable String code, @Valid @RequestBody PositionSaveRequest request) {
        return ApiResponse.success(positionService.update(code, request));
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasAuthority('position:delete')")
    @OperationLog(module = "广告点位", action = "归档", target = "#code")
    public ApiResponse<Void> archive(@PathVariable String code, @RequestParam Integer version) {
        positionService.archive(code, version);
        return ApiResponse.successMessage("点位已归档");
    }
}
