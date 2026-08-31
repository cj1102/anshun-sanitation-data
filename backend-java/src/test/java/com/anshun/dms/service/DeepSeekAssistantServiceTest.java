package com.anshun.dms.service;

import com.anshun.dms.agent.AgentToolRegistry;
import com.anshun.dms.dto.AiChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CancellationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class DeepSeekAssistantServiceTest {

    @Test
    void cancelledStreamStopsBeforeRateLimitOrDataAccess() {
        AiRateLimitService rateLimit = mock(AiRateLimitService.class);
        AiConversationService conversations = mock(AiConversationService.class);
        AiKnowledgeService knowledge = mock(AiKnowledgeService.class);
        AiUserMemoryService memory = mock(AiUserMemoryService.class);
        AgentToolRegistry tools = mock(AgentToolRegistry.class);
        AiAgentTraceService traces = mock(AiAgentTraceService.class);
        AiPendingActionService actions = mock(AiPendingActionService.class);
        DeepSeekAssistantService service = new DeepSeekAssistantService(
                "test-key", "http://127.0.0.1:9", "test-model", true,
                rateLimit, conversations, knowledge, memory, tools, traces, actions, new ObjectMapper());
        AiStreamEventSink cancelledSink = new AiStreamEventSink() {
            @Override
            public void emit(String event, Map<String, Object> data) { }

            @Override
            public boolean isCancelled() {
                return true;
            }
        };

        assertThatThrownBy(() -> service.chatStream(new AiChatRequest("测试", null, null), null, cancelledSink))
                .isInstanceOf(CancellationException.class);
        verifyNoInteractions(rateLimit, conversations, knowledge, memory, tools, traces, actions);
    }
}
