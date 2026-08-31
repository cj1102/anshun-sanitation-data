package com.anshun.dms.service;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.dto.AiAgentFeedbackRequest;
import com.anshun.dms.mapper.AiAgentFeedbackMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAgentFeedbackServiceTest {
    @Mock AiAgentFeedbackMapper mapper;
    @InjectMocks AiAgentFeedbackService service;

    @Test
    void onlyAllowsFeedbackForTheCurrentUsersRun() {
        when(mapper.selectActiveUserId("cj")).thenReturn(7);
        when(mapper.upsertForOwnedRun(12L, 7, "UP", "很好")).thenReturn(1);

        service.save("cj", 12L, new AiAgentFeedbackRequest("UP", " 很好 "));

        verify(mapper).upsertForOwnedRun(12L, 7, "UP", "很好");
    }

    @Test
    void refusesForeignOrMissingRun() {
        when(mapper.selectActiveUserId("cj")).thenReturn(7);
        when(mapper.upsertForOwnedRun(eq(99L), eq(7), eq("DOWN"), eq(null))).thenReturn(0);

        assertThatThrownBy(() -> service.save("cj", 99L, new AiAgentFeedbackRequest("DOWN", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("本人的");
    }
}
