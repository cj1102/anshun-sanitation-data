package com.anshun.dms.service;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.dto.LeaseApprovalRequest;
import com.anshun.dms.dto.LeasePageQuery;
import com.anshun.dms.dto.LeaseSaveRequest;
import com.anshun.dms.mapper.LeaseMapper;
import com.anshun.dms.vo.LeaseVO;
import com.anshun.dms.vo.PageData;
import com.anshun.dms.vo.PositionVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;

@Service
public class LeaseService {
    public static final String DRAFT = "DRAFT";
    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    private final LeaseMapper leaseMapper;
    private final PositionService positionService;

    public LeaseService(LeaseMapper leaseMapper, PositionService positionService) {
        this.leaseMapper = leaseMapper;
        this.positionService = positionService;
    }

    public PageData<LeaseVO> page(LeasePageQuery query) {
        return new PageData<>(leaseMapper.countPage(query), leaseMapper.selectPage(query));
    }

    /** Read-only detail entry used by the Agent tool registry as well as future REST detail views. */
    public LeaseVO detail(long id) {
        LeaseVO lease = leaseMapper.selectById(id);
        if (lease == null) throw BusinessException.notFound("指定的租赁合同记录不存在");
        return lease;
    }

    @Transactional
    public LeaseVO create(LeaseSaveRequest request) {
        // A draft is intentionally allowed to overlap another draft. The authoritative overlap
        // check is made under a point row lock when an auditor approves the contract.
        PositionVO position = positionService.lockActivePosition(request.adPositionCode());
        LeaseVO lease = toLease(request, null, 0, position, DRAFT);
        leaseMapper.insert(lease);
        // MyBatis does not need a generated ID for the frontend; return the persisted list-compatible record when available.
        return lease;
    }

    @Transactional
    public LeaseVO update(long id, LeaseSaveRequest request) {
        if (request.version() == null) throw BusinessException.badRequest("数据已过期，请刷新页面后再保存");
        LeaseVO existing = detail(id);
        if (!DRAFT.equals(existing.approvalStatus()) && !REJECTED.equals(existing.approvalStatus())) {
            throw BusinessException.conflict("只有草稿或已驳回的合同可以编辑");
        }
        PositionVO position = positionService.lockActivePosition(request.adPositionCode());
        LeaseVO lease = toLease(request, id, request.version(), position, existing.approvalStatus());
        if (leaseMapper.update(lease, request.version()) == 0) {
            throw BusinessException.conflict("合同已被其他用户修改，请刷新后重试");
        }
        return leaseMapper.selectById(id);
    }

    @Transactional
    public LeaseVO submitForApproval(long id, String username) {
        LeaseVO existing = detail(id);
        if (!DRAFT.equals(existing.approvalStatus()) && !REJECTED.equals(existing.approvalStatus())) {
            throw BusinessException.conflict("当前合同不能重复提交审核");
        }
        positionService.lockActivePosition(existing.adPositionCode());
        if (leaseMapper.submitForApproval(id, existing.version(), username) == 0) {
            throw BusinessException.conflict("合同已被其他用户修改，请刷新后重试");
        }
        return detail(id);
    }

    @Transactional
    @CacheEvict(cacheNames = "statistics", allEntries = true)
    public LeaseVO approve(long id, LeaseApprovalRequest request, String username) {
        return decide(id, request, username, APPROVED);
    }

    @Transactional
    @CacheEvict(cacheNames = "statistics", allEntries = true)
    public LeaseVO reject(long id, LeaseApprovalRequest request, String username) {
        String comment = request.comment();
        if (comment == null || comment.isBlank()) throw BusinessException.badRequest("驳回合同必须填写审核意见");
        return decide(id, request, username, REJECTED);
    }

    @Transactional
    @CacheEvict(cacheNames = "statistics", allEntries = true)
    public void archive(long id, int expectedVersion) {
        LeaseVO existing = detail(id);
        if (!DRAFT.equals(existing.approvalStatus()) && !REJECTED.equals(existing.approvalStatus())) {
            throw BusinessException.conflict("只有草稿或已驳回的合同可以归档；待审核或已生效合同需先完成正式作废流程");
        }
        if (leaseMapper.logicalDelete(id, expectedVersion) == 0) {
            throw BusinessException.conflict("合同状态或版本已变化，请刷新后重试");
        }
    }

    private LeaseVO decide(long id, LeaseApprovalRequest request, String username, String decision) {
        LeaseVO existing = detail(id);
        if (!PENDING.equals(existing.approvalStatus())) throw BusinessException.conflict("只有待审核合同可以进行审核操作");
        if (username.equals(existing.submittedByUsername())) {
            throw BusinessException.conflict("合同提交人不能审核自己提交的合同，请由其他审核员处理");
        }

        if (APPROVED.equals(decision)) {
            // The lock serializes approvals for the same position. After the first approval
            // commits, the second request sees the effective contract and is rejected.
            positionService.lockActivePosition(existing.adPositionCode());
            rejectOverlappingLease(existing.adPositionCode(), existing.leaseStartDate(), existing.leaseEndDate(), id);
        }
        if (leaseMapper.decideApproval(id, existing.version(), decision, username, blankToNull(request.comment())) == 0) {
            throw BusinessException.conflict("合同审核状态已变化，请刷新后重试");
        }
        return detail(id);
    }

    private LeaseVO toLease(LeaseSaveRequest request, Long id, Integer version, PositionVO position, String approvalStatus) {
        if (request.leaseEndDate().isBefore(request.leaseStartDate())) {
            throw BusinessException.badRequest("租期结束日期必须在开始日期之后");
        }
        long term = ChronoUnit.DAYS.between(request.leaseStartDate(), request.leaseEndDate()) + 1;
        if (term > 3650) throw BusinessException.badRequest("单份合同租期不能超过 10 年");
        return new LeaseVO(id, request.contractCode(), request.adPositionCode(), position.adLocation(), position.singleSideArea(),
                position.totalAdArea(), position.adSpecification(), request.lesseeCode(), request.lesseeCompany(), request.leaseRent(),
                (int) term, request.leaseStartDate(), request.leaseEndDate(), request.contractSignDate(), version, 0L,
                approvalStatus, null, null, null, null, null);
    }

    private void rejectOverlappingLease(String adPositionCode, java.time.LocalDate leaseStartDate,
                                        java.time.LocalDate leaseEndDate, Long excludeLeaseId) {
        if (leaseMapper.countOverlappingLeases(adPositionCode, leaseStartDate, leaseEndDate, excludeLeaseId) > 0) {
            throw BusinessException.conflict("该广告点位在所选租期内已有有效合同，请调整租期或选择其他点位");
        }
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
