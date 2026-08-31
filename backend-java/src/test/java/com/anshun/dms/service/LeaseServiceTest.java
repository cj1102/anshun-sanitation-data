package com.anshun.dms.service;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.dto.LeaseApprovalRequest;
import com.anshun.dms.dto.LeaseSaveRequest;
import com.anshun.dms.mapper.LeaseMapper;
import com.anshun.dms.vo.LeaseVO;
import com.anshun.dms.vo.PositionVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaseServiceTest {
    @Mock LeaseMapper leaseMapper;
    @Mock PositionService positionService;
    @InjectMocks LeaseService leaseService;

    @Test
    void createsAnEditableDraftWhileHoldingThePositionLifecycleLock() {
        LeaseSaveRequest request = request();
        when(positionService.lockActivePosition("AS-COL-039")).thenReturn(position());

        LeaseVO draft = leaseService.create(request);

        assertThat(draft.approvalStatus()).isEqualTo(LeaseService.DRAFT);
        verify(positionService).lockActivePosition("AS-COL-039");
        verify(leaseMapper, never()).countOverlappingLeases(any(), any(), any(), any());
        verify(leaseMapper).insert(any());
    }

    @Test
    void submittingDraftLocksItsPositionBeforeChangingState() {
        LeaseVO draft = lease(LeaseService.DRAFT, 3);
        LeaseVO pending = lease(LeaseService.PENDING, 4);
        when(leaseMapper.selectById(21)).thenReturn(draft, pending);
        when(positionService.lockActivePosition("AS-COL-039")).thenReturn(position());
        when(leaseMapper.submitForApproval(21, 3, "operator")).thenReturn(1);

        LeaseVO result = leaseService.submitForApproval(21, "operator");

        assertThat(result.approvalStatus()).isEqualTo(LeaseService.PENDING);
        verify(positionService).lockActivePosition("AS-COL-039");
        verify(leaseMapper).submitForApproval(21, 3, "operator");
    }

    @Test
    void approvingPendingLeaseLocksPositionAndRejectsDateOverlap() {
        LeaseVO pending = lease(LeaseService.PENDING, 3);
        when(leaseMapper.selectById(21)).thenReturn(pending);
        when(positionService.lockActivePosition("AS-COL-039")).thenReturn(position());
        when(leaseMapper.countOverlappingLeases(eq("AS-COL-039"), eq(pending.leaseStartDate()), eq(pending.leaseEndDate()), eq(21L))).thenReturn(1L);

        assertThatThrownBy(() -> leaseService.approve(21, new LeaseApprovalRequest("资料齐全"), "auditor"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已有有效合同");

        verify(positionService).lockActivePosition("AS-COL-039");
        verify(leaseMapper, never()).decideApproval(anyLong(), anyInt(), anyString(), anyString(), any());
    }

    @Test
    void approvingPendingLeasePersistsDecisionAfterConcurrencyChecks() {
        LeaseVO pending = lease(LeaseService.PENDING, 3);
        LeaseVO approved = lease(LeaseService.APPROVED, 4);
        when(leaseMapper.selectById(21)).thenReturn(pending, approved);
        when(positionService.lockActivePosition("AS-COL-039")).thenReturn(position());
        when(leaseMapper.countOverlappingLeases(eq("AS-COL-039"), eq(pending.leaseStartDate()), eq(pending.leaseEndDate()), eq(21L))).thenReturn(0L);
        when(leaseMapper.decideApproval(21, 3, LeaseService.APPROVED, "auditor", "资料齐全")).thenReturn(1);

        LeaseVO result = leaseService.approve(21, new LeaseApprovalRequest("资料齐全"), "auditor");

        assertThat(result.approvalStatus()).isEqualTo(LeaseService.APPROVED);
        verify(leaseMapper).decideApproval(21, 3, LeaseService.APPROVED, "auditor", "资料齐全");
    }

    @Test
    void rejectingPendingLeaseRequiresComment() {
        assertThatThrownBy(() -> leaseService.reject(21, new LeaseApprovalRequest("  "), "auditor"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("必须填写审核意见");
    }

    @Test
    void submitterCannotApproveTheirOwnContract() {
        LeaseVO pending = lease(LeaseService.PENDING, 3, "operator");
        when(leaseMapper.selectById(21)).thenReturn(pending);

        assertThatThrownBy(() -> leaseService.approve(21, new LeaseApprovalRequest("资料齐全"), "operator"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("提交人不能审核自己提交");

        verify(positionService, never()).lockActivePosition(any());
    }

    @Test
    void archivesDraftUsingTheVersionObservedByTheOperator() {
        when(leaseMapper.selectById(21)).thenReturn(lease(LeaseService.DRAFT, 3));
        when(leaseMapper.logicalDelete(21, 3)).thenReturn(1);

        leaseService.archive(21, 3);

        verify(leaseMapper).logicalDelete(21, 3);
    }

    @Test
    void neverArchivesAnApprovedContract() {
        when(leaseMapper.selectById(21)).thenReturn(lease(LeaseService.APPROVED, 4));

        assertThatThrownBy(() -> leaseService.archive(21, 4))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已生效合同");

        verify(leaseMapper, never()).logicalDelete(anyLong(), anyInt());
    }

    @Test
    void archiveLosesToAConcurrentSubmitOrEdit() {
        when(leaseMapper.selectById(21)).thenReturn(lease(LeaseService.DRAFT, 3));
        when(leaseMapper.logicalDelete(21, 3)).thenReturn(0);

        assertThatThrownBy(() -> leaseService.archive(21, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("状态或版本已变化");
    }

    private LeaseSaveRequest request() {
        return new LeaseSaveRequest("AI-CON-001", "AS-COL-039", "LESSEE-001", "测试承租方", BigDecimal.valueOf(12.5),
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), LocalDate.of(2026, 7, 22), null);
    }

    private LeaseVO lease(String approvalStatus, int version) {
        return lease(approvalStatus, version, null);
    }

    private LeaseVO lease(String approvalStatus, int version, String submittedByUsername) {
        return new LeaseVO(21L, "AI-CON-001", "AS-COL-039", "测试位置", "6m×3m", 18, "双面立柱式",
                "LESSEE-001", "测试承租方", BigDecimal.valueOf(12.5), 31, LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31), LocalDate.of(2026, 7, 22), version, 0L, approvalStatus,
                submittedByUsername, null, null, null, null);
    }

    private PositionVO position() {
        return new PositionVO(39L, "AS-COL-039", "测试位置", "6m×3m", 18, "双面立柱式",
                null, null, "西秀区", "测试大道", "vacant", null, 0);
    }
}
