package com.tiktel.ttelgo.security.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for logging audit events
 */
@Slf4j
@Service
public class AuditService {
    
    private final JdbcTemplate jdbcTemplate;
    
    public AuditService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * Log audit event asynchronously
     */
    @Async
    public void logAudit(String actorType, String actorIdentifier, Long userId, Long vendorId,
                        String action, String resourceType, Long resourceId,
                        String description, String ipAddress, String userAgent,
                        boolean success, String errorMessage) {
        
        try {
            String sql = "INSERT INTO audit_logs " +
                        "(actor_type, actor_identifier, user_id, vendor_id, action, resource_type, " +
                        "resource_id, description, ip_address, user_agent, success, error_message, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            jdbcTemplate.update(sql,
                    actorType, actorIdentifier, userId, vendorId, action, resourceType,
                    resourceId, description, ipAddress, userAgent, success, errorMessage,
                    LocalDateTime.now());
            
            log.debug("Audit log created: action={}, resource={}:{}", action, resourceType, resourceId);
            
        } catch (Exception e) {
            log.error("Failed to log audit event", e);
        }
    }
    
    /**
     * Log user action
     */
    public void logUserAction(Long userId, String action, String resourceType, Long resourceId,
                             String ipAddress, String userAgent) {
        logAudit("USER", String.valueOf(userId), userId, null, action, resourceType,
                resourceId, null, ipAddress, userAgent, true, null);
    }
    
    /**
     * Log vendor action
     */
    public void logVendorAction(Long vendorId, String action, String resourceType, Long resourceId,
                               String ipAddress, String userAgent) {
        logAudit("VENDOR", String.valueOf(vendorId), null, vendorId, action, resourceType,
                resourceId, null, ipAddress, userAgent, true, null);
    }
    
    /**
     * Log failed action
     */
    public void logFailedAction(String actorType, String actorIdentifier, String action,
                               String resourceType, String errorMessage, String ipAddress) {
        logAudit(actorType, actorIdentifier, null, null, action, resourceType,
                null, null, ipAddress, null, false, errorMessage);
    }
}

