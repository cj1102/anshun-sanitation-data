package com.anshun.dms.audit;

import jakarta.servlet.http.HttpServletRequest;
import com.anshun.dms.security.ClientIpResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
public class OperationLogAspect {
    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);
    private final AuditLogService auditLogService;
    private final JdbcTemplate jdbc;
    private final ClientIpResolver clientIpResolver;
    private final SpelExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public OperationLogAspect(AuditLogService auditLogService, JdbcTemplate jdbc, ClientIpResolver clientIpResolver) {
        this.auditLogService = auditLogService;
        this.jdbc = jdbc;
        this.clientIpResolver = clientIpResolver;
    }

    @Around("@annotation(operationLog)")
    public Object record(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable failure = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            failure = throwable;
            throw throwable;
        } finally {
            boolean success = failure == null && (!(result instanceof ResponseEntity<?> response) || response.getStatusCode().is2xxSuccessful());
            try {
                persist(joinPoint, operationLog, success, failure == null ? null : failure.getMessage(), System.currentTimeMillis() - start);
            } catch (Exception auditFailure) {
                log.error("Failed to persist audit log", auditFailure);
            }
        }
    }

    private void persist(ProceedingJoinPoint joinPoint, OperationLog operationLog, boolean success, String error, long duration) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken
                ? "anonymous" : authentication.getName();
        HttpServletRequest request = request();
        auditLogService.save(new AuditLogService.AuditEntry(findUserId(username), username, operationLog.module(), operationLog.action(),
                resolveTarget(joinPoint, operationLog.target()), request == null ? null : request.getMethod(), request == null ? null : request.getRequestURI(),
                MDC.get("requestId"), clientIpResolver.resolve(request), success, trim(error), duration));
    }

    private Integer findUserId(String username) {
        if ("anonymous".equals(username)) return null;
        try { return jdbc.queryForObject("SELECT user_id FROM t_user WHERE username=?", Integer.class, username); }
        catch (Exception ignored) { return null; }
    }

    private String resolveTarget(ProceedingJoinPoint joinPoint, String expression) {
        if (expression.isBlank()) return null;
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(joinPoint.getTarget(), method, joinPoint.getArgs(), parameterNameDiscoverer);
        Object value = expressionParser.parseExpression(expression).getValue(context);
        return value == null ? null : String.valueOf(value);
    }

    private HttpServletRequest request() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) return null;
        return attributes.getRequest();
    }
    private String trim(String value) { return value == null ? null : value.substring(0, Math.min(value.length(), 500)); }
}
