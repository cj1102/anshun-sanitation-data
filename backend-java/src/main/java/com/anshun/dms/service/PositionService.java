package com.anshun.dms.service;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.dto.PositionPageQuery;
import com.anshun.dms.dto.PositionSaveRequest;
import com.anshun.dms.mapper.PositionMapper;
import com.anshun.dms.vo.LeaseVO;
import com.anshun.dms.vo.PageData;
import com.anshun.dms.vo.PositionDetailVO;
import com.anshun.dms.vo.PositionVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PositionService {
    private final PositionMapper positionMapper;

    public PositionService(PositionMapper positionMapper) { this.positionMapper = positionMapper; }

    public PageData<PositionVO> page(PositionPageQuery query) {
        return new PageData<>(positionMapper.countPage(query), positionMapper.selectPage(query));
    }

    public PositionDetailVO detail(String code) {
        PositionVO position = requirePosition(code);
        List<LeaseVO> history = positionMapper.selectLeaseHistory(code);
        return new PositionDetailVO(position.adPositionId(), position.adPositionCode(), position.adLocation(), position.singleSideArea(),
                position.totalAdArea(), position.adSpecification(), position.longitude(), position.latitude(), position.district(),
                position.roadName(), position.status(), position.remark(), position.version(), positionMapper.selectValuation(code), history);
    }

    @Transactional
    @CacheEvict(cacheNames = "statistics", allEntries = true)
    public PositionVO create(PositionSaveRequest request) {
        if (positionMapper.selectByCode(request.adPositionCode()) != null) {
            throw BusinessException.conflict("该点位编码已存在，请勿重复创建");
        }
        PositionVO position = toPosition(request, null, 0);
        try {
            positionMapper.insert(position);
        } catch (DuplicateKeyException exception) {
            // The pre-check is for a friendly message; the unique index is the actual concurrency guard.
            throw BusinessException.conflict("该点位编码已存在，请勿重复创建");
        }
        return requirePosition(request.adPositionCode());
    }

    @Transactional
    @CacheEvict(cacheNames = "statistics", allEntries = true)
    public PositionVO update(String code, PositionSaveRequest request) {
        requireVersion(request.version());
        requireImmutableCode(code, request.adPositionCode());
        PositionVO existing = requirePosition(code);
        PositionVO updated = toPosition(request, existing.adPositionCode(), existing.adPositionId(), existing.version());
        int changed = positionMapper.update(code, updated, request.version());
        if (changed == 0) throw BusinessException.conflict("点位已被其他用户修改，请刷新后重试");
        return requirePosition(code);
    }

    @Transactional
    @CacheEvict(cacheNames = "statistics", allEntries = true)
    public void archive(String code, Integer expectedVersion) {
        requireVersion(expectedVersion);
        PositionVO position = lockActivePosition(code);
        if (!expectedVersion.equals(position.version())) {
            throw BusinessException.conflict("点位已被其他用户修改，请刷新后重试");
        }
        if (positionMapper.countBlockingLeases(code) > 0) {
            throw BusinessException.conflict("该点位仍有关联的草稿、待审核或有效合同，请先处理合同后再归档");
        }
        if (positionMapper.logicalDelete(code, expectedVersion) == 0) {
            throw BusinessException.conflict("点位已被其他用户修改，请刷新后重试");
        }
    }

    /** Active position data is copied into a lease so the historical contract remains self-contained. */
    public PositionVO requirePosition(String code) {
        PositionVO position = positionMapper.selectByCode(code);
        if (position == null) throw BusinessException.notFound("指定的广告点位编码 \"" + code + "\" 不存在");
        return position;
    }

    /** Must be called from an existing transaction before scheduling a contract for this position. */
    public PositionVO lockActivePosition(String code) {
        PositionVO position = positionMapper.selectActiveForUpdate(code);
        if (position == null) throw BusinessException.notFound("指定的广告点位编码 \"" + code + "\" 不存在");
        return position;
    }

    private PositionVO toPosition(PositionSaveRequest request, Long id, Integer version) {
        return toPosition(request, request.adPositionCode(), id, version);
    }

    private PositionVO toPosition(PositionSaveRequest request, String code, Long id, Integer version) {
        String status = request.status() == null || request.status().isBlank() ? "vacant" : request.status();
        return new PositionVO(id, code, request.adLocation(), request.singleSideArea(), request.totalAdArea(),
                request.adSpecification(), request.longitude(), request.latitude(), request.district(), request.roadName(), status,
                request.remark(), version);
    }

    private void requireImmutableCode(String pathCode, String requestCode) {
        if (!pathCode.equals(requestCode)) {
            throw BusinessException.badRequest("点位编码创建后不可修改，请保持请求路径与请求体中的编码一致");
        }
    }

    private void requireVersion(Integer version) {
        if (version == null) throw BusinessException.badRequest("数据已过期，请刷新页面后再保存");
    }
}
