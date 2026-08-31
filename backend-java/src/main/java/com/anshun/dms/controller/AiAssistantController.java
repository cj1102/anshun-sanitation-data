package com.anshun.dms.controller;

import com.anshun.dms.audit.OperationLog;
import com.anshun.dms.common.ApiResponse;
import com.anshun.dms.common.BusinessException;
import com.anshun.dms.dto.AiChatRequest;
import com.anshun.dms.dto.AiAgentFeedbackRequest;
import com.anshun.dms.service.AiAgentFeedbackService;
import com.anshun.dms.service.AiConversationService;
import com.anshun.dms.service.AiAgentTraceService;
import com.anshun.dms.service.AiPendingActionService;
import com.anshun.dms.service.AiStreamEventSink;
import com.anshun.dms.service.DeepSeekAssistantService;
import com.anshun.dms.vo.AiActionConfirmResponse;
import com.anshun.dms.vo.AiAgentRunVO;
import com.anshun.dms.vo.AiChatResponse;
import com.anshun.dms.vo.AiConversationDetailVO;
import com.anshun.dms.vo.AiConversationSummaryVO;
import com.anshun.dms.vo.AiPendingActionVO;
import com.anshun.dms.vo.PageData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/ai")
public class AiAssistantController {
    private final DeepSeekAssistantService assistantService;
    private final AiConversationService conversationService;
    private final AiAgentTraceService agentTraceService;
    private final AiAgentFeedbackService agentFeedbackService;
    private final AiPendingActionService pendingActionService;
    private final ThreadPoolTaskExecutor aiStreamTaskExecutor;
    private final long streamTimeoutMs;
    public AiAssistantController(DeepSeekAssistantService assistantService, AiConversationService conversationService,
                                 AiAgentTraceService agentTraceService, AiAgentFeedbackService agentFeedbackService,
                                 AiPendingActionService pendingActionService,
                                 @Qualifier("aiStreamTaskExecutor") ThreadPoolTaskExecutor aiStreamTaskExecutor,
                                 @Value("${app.ai.streaming.sse-timeout-ms:300000}") long streamTimeoutMs) {
        this.assistantService = assistantService;
        this.conversationService = conversationService;
        this.agentTraceService = agentTraceService;
        this.agentFeedbackService = agentFeedbackService;
        this.pendingActionService = pendingActionService;
        this.aiStreamTaskExecutor = aiStreamTaskExecutor;
        this.streamTimeoutMs = streamTimeoutMs;
    }

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "AI 助手", action = "对话", target = "#request.page")
    public ApiResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request, Authentication authentication) {
        return ApiResponse.success(assistantService.chat(request, authentication));
    }

    /**
     * POST is used because the native browser EventSource API cannot attach the JWT Authorization header.
     * The dedicated worker retains requestId so streaming traces remain correlated with normal request logs.
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "AI 助手", action = "发起流式对话", target = "#request.page")
    public SseEmitter chatStream(@Valid @RequestBody AiChatRequest request, Authentication authentication) {
        SseEmitter emitter = new SseEmitter(streamTimeoutMs);
        AtomicBoolean clientDisconnected = new AtomicBoolean(false);
        AtomicBoolean workerFinished = new AtomicBoolean(false);
        AtomicReference<Future<?>> task = new AtomicReference<>();
        Runnable cancelWorker = () -> {
            clientDisconnected.set(true);
            Future<?> running = task.get();
            if (!workerFinished.get() && running != null) running.cancel(true);
        };
        emitter.onCompletion(cancelWorker);
        emitter.onTimeout(cancelWorker);
        emitter.onError(ignored -> cancelWorker.run());
        String requestId = MDC.get("requestId");
        try {
            Future<?> submitted = aiStreamTaskExecutor.submit(() -> {
                if (requestId != null) MDC.put("requestId", requestId);
                try {
                    AiStreamEventSink sink = new AiStreamEventSink() {
                        @Override public void emit(String event, Map<String, Object> data) {
                            send(emitter, clientDisconnected, event, data);
                        }
                        @Override public boolean isCancelled() {
                            return clientDisconnected.get() || Thread.currentThread().isInterrupted();
                        }
                    };
                    assistantService.chatStream(request, authentication, sink);
                    workerFinished.set(true);
                    if (!clientDisconnected.get()) emitter.complete();
                } catch (Exception exception) {
                    send(emitter, clientDisconnected, "error", Map.of("message", safeMessage(exception)));
                    workerFinished.set(true);
                    if (!clientDisconnected.get()) emitter.complete();
                } finally {
                    workerFinished.set(true);
                    MDC.remove("requestId");
                }
            });
            task.set(submitted);
            if (clientDisconnected.get() && !workerFinished.get()) submitted.cancel(true);
        } catch (TaskRejectedException exception) {
            clientDisconnected.set(true);
            throw BusinessException.tooManyRequests("AI 请求较多，请稍后重试");
        }
        return emitter;
    }

    private void send(SseEmitter emitter, AtomicBoolean clientDisconnected, String event, Map<String, Object> data) {
        if (clientDisconnected.get()) return;
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException exception) {
            clientDisconnected.set(true);
            Thread.currentThread().interrupt();
        }
    }

    private String safeMessage(Exception exception) {
        if (exception instanceof BusinessException businessException) return businessException.getMessage();
        String requestId = MDC.get("requestId");
        return "AI 流式响应异常，请稍后重试" + (requestId == null ? "" : "（请求 ID：" + requestId + "）");
    }

    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageData<AiConversationSummaryVO>> conversations(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于 0") @Max(value = 50, message = "每页数量不能超过 50") int pageSize,
            Authentication authentication) {
        return ApiResponse.success(conversationService.list(authentication.getName(), page, pageSize));
    }

    @GetMapping("/agent-runs")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageData<AiAgentRunVO>> agentRuns(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页数量必须大于 0") @Max(value = 50, message = "每页数量不能超过 50") int pageSize,
            Authentication authentication) {
        return ApiResponse.success(agentTraceService.list(authentication.getName(), page, pageSize));
    }

    @PostMapping("/agent-runs/{runId}/feedback")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "AI Agent", action = "提交回答反馈", target = "#runId")
    public ApiResponse<Void> feedback(@PathVariable @Min(value = 1, message = "运行编号必须大于 0") long runId,
                                      @Valid @RequestBody AiAgentFeedbackRequest request,
                                      Authentication authentication) {
        agentFeedbackService.save(authentication.getName(), runId, request);
        return ApiResponse.successMessage("反馈已保存，感谢你的帮助");
    }

    @GetMapping("/actions/pending")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<java.util.List<AiPendingActionVO>> pendingActions(Authentication authentication) {
        return ApiResponse.success(pendingActionService.pending(authentication.getName()));
    }

    @PostMapping("/actions/{actionId}/confirm")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "AI Agent", action = "确认执行待确认操作", target = "#actionId")
    public ApiResponse<AiActionConfirmResponse> confirmAction(@PathVariable String actionId, Authentication authentication) {
        return ApiResponse.success(pendingActionService.confirm(authentication.getName(), actionId, authentication));
    }

    @DeleteMapping("/actions/{actionId}")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "AI Agent", action = "取消待确认操作", target = "#actionId")
    public ApiResponse<Void> cancelAction(@PathVariable String actionId, Authentication authentication) {
        pendingActionService.cancel(authentication.getName(), actionId);
        return ApiResponse.successMessage("已取消待确认操作");
    }

    @GetMapping("/conversations/{conversationId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AiConversationDetailVO> conversation(@PathVariable @Min(value = 1, message = "对话编号必须大于 0") long conversationId,
                                                               Authentication authentication) {
        return ApiResponse.success(conversationService.detail(authentication.getName(), conversationId));
    }

    @DeleteMapping("/conversations/{conversationId}")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "AI 助手", action = "删除对话", target = "#conversationId")
    public ApiResponse<Void> deleteConversation(@PathVariable @Min(value = 1, message = "对话编号必须大于 0") long conversationId,
                                                 Authentication authentication) {
        conversationService.delete(authentication.getName(), conversationId);
        return ApiResponse.successMessage("对话已删除");
    }
}
