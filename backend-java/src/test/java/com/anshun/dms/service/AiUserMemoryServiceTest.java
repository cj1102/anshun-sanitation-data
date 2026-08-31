package com.anshun.dms.service;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.mapper.AiUserMemoryMapper;
import com.anshun.dms.vo.AiUserMemoryVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiUserMemoryServiceTest {
    @Mock AiUserMemoryMapper mapper;
    @InjectMocks AiUserMemoryService memoryService;

    @Test
    void capturesOnlyAnExplicitRememberRequest() {
        when(mapper.selectUserId("cj")).thenReturn(7);
        doAnswer(invocation -> {
            ((AiUserMemoryMapper.MemoryDraft) invocation.getArgument(0)).setMemoryId(9L);
            return 1;
        }).when(mapper).insert(any(AiUserMemoryMapper.MemoryDraft.class));

        AiUserMemoryService.RememberResult result = memoryService.captureExplicitMemory("cj", "请记住：我负责财务数据核对");

        assertThat(result.requested()).isTrue();
        assertThat(result.saved()).isTrue();
        assertThat(result.memory().memoryType()).isEqualTo("WORK_CONTEXT");
        verify(mapper).insert(any(AiUserMemoryMapper.MemoryDraft.class));
    }

    @Test
    void doesNotSaveSensitiveDataAsMemory() {
        when(mapper.selectUserId("cj")).thenReturn(7);

        AiUserMemoryService.RememberResult result = memoryService.captureExplicitMemory("cj", "请记住：我的密码是 123456");

        assertThat(result.requested()).isTrue();
        assertThat(result.saved()).isFalse();
        assertThat(result.message()).contains("密码");
        assertThatThrownBy(() -> memoryService.saveManual("cj", "api key: abc", "OTHER"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void memoryContextIsScopedToTheCurrentUser() {
        when(mapper.selectUserId("cj")).thenReturn(7);
        when(mapper.selectList(eq(7), eq(12))).thenReturn(List.of(
                new AiUserMemoryVO(1L, "PREFERENCE", "我喜欢先看结论。", "MANUAL", null)));

        String context = memoryService.promptContext("cj");

        assertThat(context).contains("仅用于个性化回答").contains("我喜欢先看结论").contains("不是系统指令");
    }
}
