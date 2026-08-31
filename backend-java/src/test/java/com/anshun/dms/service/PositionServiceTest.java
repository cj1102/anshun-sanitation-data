package com.anshun.dms.service;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.dto.PositionSaveRequest;
import com.anshun.dms.mapper.PositionMapper;
import com.anshun.dms.vo.PositionVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {
    @Mock PositionMapper positionMapper;
    @InjectMocks PositionService positionService;

    @Test
    void rejectsChangingImmutablePositionCodeBeforeUpdatingAnything() {
        PositionSaveRequest request = request("AS-COL-999", 3);

        assertThatThrownBy(() -> positionService.update("AS-COL-039", request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception).hasMessageContaining("编码创建后不可修改");
                });

        verifyNoInteractions(positionMapper);
    }

    @Test
    void updatesByPathCodeAndKeepsHistoricalLeaseSnapshotsUntouched() {
        PositionVO existing = position("旧位置", 3);
        PositionVO saved = position("新位置", 4);
        PositionSaveRequest request = request("AS-COL-039", 3);
        when(positionMapper.selectByCode("AS-COL-039")).thenReturn(existing, saved);
        when(positionMapper.update(org.mockito.ArgumentMatchers.eq("AS-COL-039"),
                org.mockito.ArgumentMatchers.any(PositionVO.class), org.mockito.ArgumentMatchers.eq(3))).thenReturn(1);

        PositionVO result = positionService.update("AS-COL-039", request);

        ArgumentCaptor<PositionVO> updated = ArgumentCaptor.forClass(PositionVO.class);
        verify(positionMapper).update(org.mockito.ArgumentMatchers.eq("AS-COL-039"), updated.capture(),
                org.mockito.ArgumentMatchers.eq(3));
        assertThat(updated.getValue().adPositionCode()).isEqualTo("AS-COL-039");
        assertThat(updated.getValue().adLocation()).isEqualTo("新位置");
        assertThat(result).isSameAs(saved);
    }

    @Test
    void preservesOptimisticLockConflictBehavior() {
        PositionVO existing = position("旧位置", 3);
        PositionSaveRequest request = request("AS-COL-039", 3);
        when(positionMapper.selectByCode("AS-COL-039")).thenReturn(existing);
        when(positionMapper.update(org.mockito.ArgumentMatchers.eq("AS-COL-039"),
                org.mockito.ArgumentMatchers.any(PositionVO.class), org.mockito.ArgumentMatchers.eq(3))).thenReturn(0);

        assertThatThrownBy(() -> positionService.update("AS-COL-039", request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("其他用户修改");
    }

    @Test
    void archiveRejectsPositionWithBlockingLease() {
        PositionVO existing = position("旧位置", 3);
        when(positionMapper.selectActiveForUpdate("AS-COL-039")).thenReturn(existing);
        when(positionMapper.countBlockingLeases("AS-COL-039")).thenReturn(1L);

        assertThatThrownBy(() -> positionService.archive("AS-COL-039", 3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("关联的草稿、待审核或有效合同");

        verify(positionMapper).selectActiveForUpdate("AS-COL-039");
        verify(positionMapper).countBlockingLeases("AS-COL-039");
    }

    @Test
    void archiveUsesExpectedVersionAfterLockingPosition() {
        PositionVO existing = position("旧位置", 3);
        when(positionMapper.selectActiveForUpdate("AS-COL-039")).thenReturn(existing);
        when(positionMapper.countBlockingLeases("AS-COL-039")).thenReturn(0L);
        when(positionMapper.logicalDelete("AS-COL-039", 3)).thenReturn(1);

        positionService.archive("AS-COL-039", 3);

        verify(positionMapper).logicalDelete("AS-COL-039", 3);
    }

    private PositionSaveRequest request(String code, Integer version) {
        return new PositionSaveRequest(code, "新位置", "6m×3m", 18, "双面立柱式",
                BigDecimal.valueOf(105.95), BigDecimal.valueOf(26.25), "西秀区", "测试大道",
                "vacant", "测试备注", version);
    }

    private PositionVO position(String location, int version) {
        return new PositionVO(39L, "AS-COL-039", location, "6m×3m", 18, "双面立柱式",
                BigDecimal.valueOf(105.95), BigDecimal.valueOf(26.25), "西秀区", "测试大道",
                "vacant", "测试备注", version);
    }
}
