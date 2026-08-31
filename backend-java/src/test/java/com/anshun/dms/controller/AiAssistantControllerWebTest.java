package com.anshun.dms.controller;

import com.anshun.dms.security.JwtTokenService;
import com.anshun.dms.service.AiAgentTraceService;
import com.anshun.dms.service.AiAgentFeedbackService;
import com.anshun.dms.service.AiConversationService;
import com.anshun.dms.service.AiPendingActionService;
import com.anshun.dms.service.AiStreamEventSink;
import com.anshun.dms.service.DeepSeekAssistantService;
import com.anshun.dms.service.UserTokenStateService;
import com.anshun.dms.vo.AiChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(AiAssistantController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiAssistantControllerWebTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean DeepSeekAssistantService assistantService;
    @MockitoBean AiConversationService conversationService;
    @MockitoBean AiAgentTraceService agentTraceService;
    @MockitoBean AiAgentFeedbackService agentFeedbackService;
    @MockitoBean AiPendingActionService pendingActionService;
    @MockitoBean JwtTokenService jwtTokenService;
    @MockitoBean UserTokenStateService userTokenStateService;
    @MockitoBean(name = "aiStreamTaskExecutor") ThreadPoolTaskExecutor aiStreamTaskExecutor;

    @Test
    void chatRejectsBlankQuestion() throws Exception {
        mockMvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void chatReturnsUnifiedResponse() throws Exception {
        when(assistantService.chat(any(), any())).thenReturn(new AiChatResponse("你好，我可以帮助解释系统功能。", "deepseek-v4-pro"));
        mockMvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"系统有哪些角色？\",\"page\":\"/dashboard\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.model").value("deepseek-v4-pro"));
    }

    @Test
    void chatStreamUsesServerSentEvents() throws Exception {
        when(aiStreamTaskExecutor.submit(any(Runnable.class))).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return CompletableFuture.completedFuture(null);
        });
        doAnswer(invocation -> {
            AiStreamEventSink sink = invocation.getArgument(2);
            sink.emit("status", java.util.Map.of("stage", "正在思考…"));
            sink.emit("delta", java.util.Map.of("content", "流式回答"));
            sink.emit("done", java.util.Map.of("answer", "流式回答", "conversationId", 1L));
            return null;
        }).when(assistantService).chatStream(any(), any(), any());

        MvcResult result = mockMvc.perform(post("/api/ai/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"请说明系统功能\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:status")))
                .andExpect(response -> org.junit.jupiter.api.Assertions.assertTrue(
                        new String(response.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8).contains("流式回答")));
    }

    @Test
    void chatStreamReturnsTooManyRequestsWhenExecutorIsSaturated() throws Exception {
        when(aiStreamTaskExecutor.submit(any(Runnable.class)))
                .thenThrow(new TaskRejectedException("executor full"));

        mockMvc.perform(post("/api/ai/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"请说明系统功能\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("AI 请求较多，请稍后重试"));
    }

    @Test
    void conversationListRejectsInvalidPage() throws Exception {
        mockMvc.perform(get("/api/ai/conversations").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void feedbackRejectsAnUnknownRating() throws Exception {
        mockMvc.perform(post("/api/ai/agent-runs/1/feedback").contentType(MediaType.APPLICATION_JSON).content("{\"rating\":\"MAYBE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
