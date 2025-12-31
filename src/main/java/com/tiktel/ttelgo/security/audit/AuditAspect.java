package com.tiktel.ttelgo.security.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * AOP aspect for automatic audit logging
 */
@Slf4j
@Aspect
@Component
public class AuditAspect {
    
    private final AuditService auditService;
    
    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }
    
    /**
     * Log successful order creation
     */
    @AfterReturning(
            pointcut = "execution(* com.tiktel.ttelgo.order.application.OrderService.createB2COrder(..))",
            returning = "result")
    public void logOrderCreation(JoinPoint joinPoint, Object result) {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                auditService.logAudit(
                        "USER",
                        "system",
                        null,
                        null,
                        "CREATE_ORDER",
                        "ORDER",
                        null,
                        "B2C order created",
                        request.getRemoteAddr(),
                        request.getHeader("User-Agent"),
                        true,
                        null
                );
            }
        } catch (Exception e) {
            log.error("Error logging audit event", e);
        }
    }
    
    /**
     * Log failed operations
     */
    @AfterThrowing(
            pointcut = "execution(* com.tiktel.ttelgo..*.*(..)) && " +
                      "@annotation(org.springframework.web.bind.annotation.PostMapping)",
            throwing = "exception")
    public void logFailedOperation(JoinPoint joinPoint, Exception exception) {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                auditService.logFailedAction(
                        "UNKNOWN",
                        "system",
                        "FAILED_OPERATION",
                        joinPoint.getSignature().getName(),
                        exception.getMessage(),
                        request.getRemoteAddr()
                );
            }
        } catch (Exception e) {
            log.error("Error logging failed operation", e);
        }
    }
    
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}

