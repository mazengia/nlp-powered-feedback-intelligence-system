package com.maze.nlpcustomerffeedbackanalyzer.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maze.nlpcustomerffeedbackanalyzer.feedback.Feedback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Value("${audit.enabled:true}")
    private boolean auditEnabled;

    @Value("${audit.log-changes:true}")
    private boolean logChanges;

    @AfterReturning(pointcut = "@annotation(auditable) && execution(* com.maze.nlpcustomerffeedbackanalyzer.feedback.FeedbackService.*(..))", returning = "result")
    public void auditFeedbackOperation(JoinPoint joinPoint, Auditable auditable, Object result) {
        if (!auditEnabled) {
            return;
        }

        try {
            String methodName = joinPoint.getSignature().getName();
            Object[] args = joinPoint.getArgs();

            if (methodName.contains("analyze")) {
                String operation = methodName.contains("Batch") ? "BATCH_ANALYZE" : "ANALYZE";
                String username = getUsername();
                String ipAddress = getClientIp();

                if (result instanceof Feedback feedback) {
                    AuditLog auditLog = AuditLog.builder()
                            .entityType("Feedback")
                            .entityId(feedback.getId())
                            .operation(AuditLog.Operation.CREATE)
                            .createdBy(username)
                            .ipAddress(ipAddress)
                            .details(String.format("Analyzed feedback: sentiment=%s, topic=%s",
                                    feedback.getSentiment(), feedback.getPrimaryTopic()))
                            .build();

                    if (logChanges && feedback.getText() != null) {
                        auditLog.setNewValue(feedback.getText().substring(0, Math.min(500, feedback.getText().length())));
                    }

                    auditLogRepository.save(auditLog);
                    log.debug("Audit logged for Feedback {}: {}", feedback.getId(), operation);
                }
            }
        } catch (Exception e) {
            log.warn("Audit logging failed: {}", e.getMessage());
        }
    }

    private String getUsername() {
        try {
            // Try to get from SecurityContext if Spring Security is available
            Class<?> securityContextHolder = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            Object context = securityContextHolder.getMethod("getContext").invoke(null);
            if (context != null) {
                Object authentication = context.getClass().getMethod("getAuthentication").invoke(context);
                if (authentication != null) {
                    Object principal = authentication.getClass().getMethod("getName").invoke(authentication);
                    if (principal != null) {
                        return principal.toString();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Spring Security not available: {}", e.getMessage());
        }
        return "ANONYMOUS";
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String ip = attributes.getRequest().getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = attributes.getRequest().getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            log.debug("Could not get client IP: {}", e.getMessage());
        }
        return "UNKNOWN";
    }
}

