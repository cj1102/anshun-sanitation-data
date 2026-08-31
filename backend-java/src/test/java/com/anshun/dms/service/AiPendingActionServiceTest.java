package com.anshun.dms.service;

import com.anshun.dms.dto.PositionSaveRequest;
import com.anshun.dms.mapper.AiPendingActionMapper;
import com.anshun.dms.vo.AiActionConfirmResponse;
import com.anshun.dms.vo.PositionVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiPendingActionServiceTest {
    @Mock AiPendingActionMapper mapper;
    @Mock PositionService positionService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void preparingAnActionOnlyStoresDraftAndNeverWritesBusinessData() {
        when(mapper.selectUserId("operator")).thenReturn(8);
        AiPendingActionService service = service();

        var action = service.prepareCreatePosition("operator", 12L, "/positions", request());

        assertThat(action.actionType()).isEqualTo(AiPendingActionService.CREATE_AD_POSITION);
        assertThat(action.status()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(action.actionId()).hasSize(32);
        verify(mapper).insert(any(AiPendingActionMapper.ActionDraft.class));
        verify(positionService, never()).create(any());
    }

    @Test
    void confirmedActionCallsExistingBusinessServiceExactlyOnce() throws Exception {
        when(mapper.selectUserId("operator")).thenReturn(8);
        String payload = objectMapper.writeValueAsString(request());
        AiPendingActionMapper.ActionRecord pending = new AiPendingActionMapper.ActionRecord("a".repeat(32), 8, 12L,
                null, AiPendingActionService.CREATE_AD_POSITION, "position:create", "PENDING_CONFIRMATION", payload,
                sha256(payload), null, "待确认：新增广告点位 AS-NEW-001", "/positions", null,
                LocalDateTime.now().plusMinutes(5), null, null, null, LocalDateTime.now());
        when(mapper.selectForUser(8, pending.actionId())).thenReturn(pending);
        when(mapper.claimForExecution(8, pending.actionId())).thenReturn(1);
        when(mapper.markExecuted(eq(8), eq(pending.actionId()), anyString())).thenReturn(1);
        when(positionService.create(any(PositionSaveRequest.class))).thenReturn(new PositionVO(100L, "AS-NEW-001", "测试路口", "6m×3m",
                18, "双面立柱式", BigDecimal.valueOf(106.7), BigDecimal.valueOf(26.5), "西秀区", "测试大道", "vacant", null, 0));

        AiActionConfirmResponse result = service().confirm("operator", pending.actionId(),
                new UsernamePasswordAuthenticationToken("operator", "n/a", List.of(new SimpleGrantedAuthority("position:create"))));

        assertThat(result.status()).isEqualTo("EXECUTED");
        assertThat(result.result()).containsEntry("adPositionCode", "AS-NEW-001");
        verify(positionService).create(any(PositionSaveRequest.class));
        verify(mapper).markExecuted(eq(8), eq(pending.actionId()), any());
    }

    private AiPendingActionService service() {
        return new AiPendingActionService(mapper, positionService, objectMapper, validator, 10);
    }

    private PositionSaveRequest request() {
        return new PositionSaveRequest("AS-NEW-001", "测试路口", "6m×3m", 18, "双面立柱式",
                BigDecimal.valueOf(106.7), BigDecimal.valueOf(26.5), "西秀区", "测试大道", "vacant", null, null);
    }

    private String sha256(String value) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder(64);
        for (byte item : bytes) result.append(String.format("%02x", item));
        return result.toString();
    }
}
