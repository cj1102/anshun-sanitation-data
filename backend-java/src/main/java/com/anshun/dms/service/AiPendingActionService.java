package com.anshun.dms.service;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.dto.PositionSaveRequest;
import com.anshun.dms.mapper.AiPendingActionMapper;
import com.anshun.dms.vo.AiActionConfirmResponse;
import com.anshun.dms.vo.AiPendingActionVO;
import com.anshun.dms.vo.PositionVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Human-in-the-loop boundary for Agent writes. A model may only create a pending draft here;
 * only a separate, authenticated confirmation endpoint can invoke an existing business Service.
 */
@Service
public class AiPendingActionService {
    public static final String CREATE_AD_POSITION = "CREATE_AD_POSITION";
    private static final String POSITION_CREATE_PERMISSION = "position:create";

    private final AiPendingActionMapper mapper;
    private final PositionService positionService;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final int confirmationTtlMinutes;

    public AiPendingActionService(AiPendingActionMapper mapper, PositionService positionService, ObjectMapper objectMapper,
                                  Validator validator,
                                  @Value("${app.ai.action-confirmation-ttl-minutes:10}") int confirmationTtlMinutes) {
        this.mapper = mapper;
        this.positionService = positionService;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.confirmationTtlMinutes = Math.max(1, Math.min(60, confirmationTtlMinutes));
    }

    public AiPendingActionVO prepareCreatePosition(String username, long agentRunId, String pageContext,
                                                    PositionSaveRequest request) {
        validate(request);
        int userId = requireUserId(username);
        String payloadJson = writeJson(request);
        String actionId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(confirmationTtlMinutes);
        String summary = "待确认：新增广告点位 " + request.adPositionCode();
        mapper.insert(new AiPendingActionMapper.ActionDraft(actionId, userId, agentRunId, CREATE_AD_POSITION,
                POSITION_CREATE_PERMISSION, payloadJson, sha256(payloadJson), summary, trim(pageContext, 100),
                trim(MDC.get("requestId"), 80), expiresAt));
        return toView(actionId, CREATE_AD_POSITION, summary, request, POSITION_CREATE_PERMISSION,
                "PENDING_CONFIRMATION", expiresAt);
    }

    public List<AiPendingActionVO> pending(String username) {
        int userId = requireUserId(username);
        return mapper.selectPendingForUser(userId).stream().map(this::toView).toList();
    }

    /**
     * Idempotent after success: retrying confirmation returns the original stored result without writing again.
     * The state claim, business write and EXECUTED marker share one transaction, so a process/database failure
     * cannot leave a committed point creation behind an EXECUTING action record.
     */
    @Transactional
    public AiActionConfirmResponse confirm(String username, String actionId, Authentication authentication) {
        int userId = requireUserId(username);
        AiPendingActionMapper.ActionRecord action = requireAction(userId, actionId);
        ensurePermission(authentication, action.requiredPermission());
        if ("EXECUTED".equals(action.status())) return completedResponse(action);
        if (!"PENDING_CONFIRMATION".equals(action.status())) {
            throw BusinessException.conflict("该操作当前状态为 " + action.status() + "，不能再次确认");
        }
        if (!action.expiresAt().isAfter(LocalDateTime.now())) {
            mapper.markExpired(userId, actionId);
            throw BusinessException.badRequest("该待确认操作已过期，请重新让 AI 生成草稿");
        }
        if (!sha256(action.payloadJson()).equals(action.payloadHash())) {
            throw BusinessException.unavailable("待确认操作完整性校验失败，已拒绝执行");
        }
        if (mapper.claimForExecution(userId, actionId) == 0) {
            AiPendingActionMapper.ActionRecord latest = requireAction(userId, actionId);
            if ("EXECUTED".equals(latest.status())) return completedResponse(latest);
            throw BusinessException.conflict("该操作正在执行或已失效，请刷新后查看状态");
        }

        Map<String, Object> result = execute(action);
        if (mapper.markExecuted(userId, actionId, writeJson(result)) != 1) {
            throw BusinessException.unavailable("操作执行状态写入失败，事务已回滚，请稍后重试");
        }
        return new AiActionConfirmResponse(actionId, "EXECUTED", "已确认并执行：" + action.summary(), result);
    }

    public void cancel(String username, String actionId) {
        int userId = requireUserId(username);
        if (mapper.cancel(userId, actionId) == 0) {
            AiPendingActionMapper.ActionRecord action = requireAction(userId, actionId);
            if (!"CANCELLED".equals(action.status())) throw BusinessException.conflict("该操作已不处于待确认状态");
        }
    }

    public void bindConversation(List<AiPendingActionVO> actions, long conversationId) {
        if (actions == null || actions.isEmpty()) return;
        mapper.bindConversation(actions.stream().map(AiPendingActionVO::actionId).toList(), conversationId);
    }

    public void cancelPendingForRun(long agentRunId, String errorMessage) {
        mapper.cancelPendingForRun(agentRunId, trim(errorMessage, 500));
    }

    private Map<String, Object> execute(AiPendingActionMapper.ActionRecord action) {
        if (CREATE_AD_POSITION.equals(action.actionType())) {
            PositionSaveRequest request = readPositionRequest(action.payloadJson());
            PositionVO position = positionService.create(request);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("adPositionCode", position.adPositionCode());
            result.put("adLocation", position.adLocation());
            result.put("status", position.status());
            result.put("message", "广告点位已创建");
            return result;
        }
        throw BusinessException.badRequest("不支持的待确认操作类型");
    }

    private AiActionConfirmResponse completedResponse(AiPendingActionMapper.ActionRecord action) {
        return new AiActionConfirmResponse(action.actionId(), "EXECUTED", "该操作已执行，无需重复提交", readResult(action.resultJson()));
    }

    private AiPendingActionMapper.ActionRecord requireAction(int userId, String actionId) {
        AiPendingActionMapper.ActionRecord action = mapper.selectForUser(userId, actionId);
        if (action == null) throw BusinessException.notFound("待确认操作不存在或无权访问");
        return action;
    }

    private int requireUserId(String username) {
        Integer userId = mapper.selectUserId(username);
        if (userId == null) throw BusinessException.notFound("当前用户不存在或已禁用");
        return userId;
    }

    private void ensurePermission(Authentication authentication, String permission) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getAuthorities().stream()
                .noneMatch(authority -> permission.equals(authority.getAuthority()))) {
            throw BusinessException.badRequest("当前用户无权确认该操作");
        }
    }

    private void validate(PositionSaveRequest request) {
        Set<ConstraintViolation<PositionSaveRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) throw BusinessException.badRequest(violations.iterator().next().getMessage());
        if (request.totalAdArea() > 100000) throw BusinessException.badRequest("点位总面积不能超过 100000 平方米");
    }

    private PositionSaveRequest readPositionRequest(String json) {
        try { return objectMapper.readValue(json, PositionSaveRequest.class); }
        catch (JsonProcessingException exception) { throw BusinessException.unavailable("待确认操作数据无法解析，已拒绝执行"); }
    }

    private Map<String, Object> readResult(String json) {
        if (!StringUtils.hasText(json)) return Map.of();
        try { return objectMapper.readValue(json, new TypeReference<>() { }); }
        catch (JsonProcessingException exception) { return Map.of(); }
    }

    private AiPendingActionVO toView(AiPendingActionMapper.ActionRecord action) {
        if (CREATE_AD_POSITION.equals(action.actionType())) {
            return toView(action.actionId(), action.actionType(), action.summary(), readPositionRequest(action.payloadJson()),
                    action.requiredPermission(), action.status(), action.expiresAt());
        }
        return new AiPendingActionVO(action.actionId(), action.actionType(), "待确认操作", action.summary(), Map.of(),
                action.requiredPermission(), action.status(), action.expiresAt());
    }

    private AiPendingActionVO toView(String actionId, String actionType, String summary, PositionSaveRequest request,
                                     String permission, String status, LocalDateTime expiresAt) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("adPositionCode", request.adPositionCode());
        fields.put("adLocation", request.adLocation());
        fields.put("singleSideArea", request.singleSideArea());
        fields.put("totalAdArea", request.totalAdArea());
        fields.put("adSpecification", request.adSpecification());
        addIfPresent(fields, "district", request.district());
        addIfPresent(fields, "roadName", request.roadName());
        addIfPresent(fields, "longitude", request.longitude());
        addIfPresent(fields, "latitude", request.latitude());
        addIfPresent(fields, "status", request.status() == null ? "vacant" : request.status());
        addIfPresent(fields, "remark", request.remark());
        return new AiPendingActionVO(actionId, actionType, "新增广告点位（待确认）", summary, fields, permission, status, expiresAt);
    }

    private void addIfPresent(Map<String, Object> fields, String key, Object value) {
        if (value != null && (!(value instanceof String text) || StringUtils.hasText(text))) fields.put(key, value);
    }
    private String writeJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw BusinessException.unavailable("待确认操作序列化失败"); }
    }
    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte item : hash) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 is unavailable", exception); }
    }
    private String trim(String value, int max) {
        if (!StringUtils.hasText(value)) return null;
        return value.substring(0, Math.min(value.length(), max));
    }
}
