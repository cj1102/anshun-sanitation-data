package com.anshun.dms.service;

import com.anshun.dms.dto.AiAgentEvaluationCaseRequest;
import com.anshun.dms.mapper.AiAgentEvaluationMapper;
import com.anshun.dms.vo.AiAgentEvaluationCaseVO;
import com.anshun.dms.vo.AiChatResponse;
import com.anshun.dms.vo.AiToolCallVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAgentEvaluationServiceTest {
    @Mock AiAgentEvaluationMapper mapper;
    @Mock DeepSeekAssistantService assistantService;
    @InjectMocks AiAgentEvaluationService service;

    @Test
    void scoresAReadOnlyToolCaseWithoutExposingWritePermission() {
        AiAgentEvaluationCaseVO evaluationCase = new AiAgentEvaluationCaseVO(5L, "概览工具", "请查询概览", "/dashboard",
                "get_dashboard_overview", "广告,点位", true, "admin", null);
        when(mapper.selectCase(5L)).thenReturn(evaluationCase);
        when(assistantService.chatForEvaluation(any(), any())).thenAnswer(invocation -> {
            var auth = (org.springframework.security.core.Authentication) invocation.getArgument(1);
            assertThat(auth.getAuthorities()).extracting("authority")
                    .contains("position:create", com.anshun.dms.agent.AgentToolRegistry.READ_ONLY_AUTHORITY);
            return new AiChatResponse("当前广告点位数据已查询。", "test-model", 2L, false, null, 8L,
                    List.of(new AiToolCallVO("get_dashboard_overview", "ok", true, 1)), List.of());
        });
        doAnswer(invocation -> { ((AiAgentEvaluationMapper.EvaluationResultDraft) invocation.getArgument(0)).setResultId(9L); return 1; })
                .when(mapper).insertResult(any(AiAgentEvaluationMapper.EvaluationResultDraft.class));

        var authentication = new UsernamePasswordAuthenticationToken("admin", "", List.of(
                new SimpleGrantedAuthority("position:create"), new SimpleGrantedAuthority("stats:view")));
        var result = service.run(5L, authentication);

        assertThat(result.passed()).isTrue();
        assertThat(result.actualTools()).isEqualTo("get_dashboard_overview");
        verify(mapper).insertResult(any(AiAgentEvaluationMapper.EvaluationResultDraft.class));
        verify(assistantService).chatForEvaluation(any(), any());
    }

    @Test
    void rejectsAnEvaluationCaseWithoutAnyExpectation() {
        assertThatThrownBy(() -> service.create(new AiAgentEvaluationCaseRequest("空用例", "测试", "/dashboard", null, null), "admin"))
                .hasMessageContaining("至少填写");
    }
}
