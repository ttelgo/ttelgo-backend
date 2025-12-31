package com.tiktel.ttelgo.common.exception;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.plan.api.dto.ListBundlesResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null, request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), null, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, Object>> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .collect(Collectors.toList());

        Map<String, Object> details = new HashMap<>();
        details.put("fieldErrors", fieldErrors);

        return build(HttpStatus.BAD_REQUEST, "Validation failed", details, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("violations", ex.getConstraintViolations().stream().map(v -> {
            Map<String, Object> m = new HashMap<>();
            m.put("path", String.valueOf(v.getPropertyPath()));
            m.put("message", v.getMessage());
            return m;
        }).collect(Collectors.toList()));

        return build(HttpStatus.BAD_REQUEST, "Validation failed", details, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", null, request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Request size exceeds maximum allowed", null, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported for this endpoint", null, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        // Security: Do not expose database constraint violation details to clients
        // Log the full exception for debugging
        log.warn("Data integrity violation: {}", ex.getMessage());
        if (ex.getRootCause() != null) {
            log.warn("Root cause: {}", ex.getRootCause().getMessage());
        }
        // Log the full exception stack trace for debugging constraint names
        log.debug("Full exception details:", ex);
        
        // Try to provide a more specific error message for common cases
        String errorMessage = "Invalid data provided";
        String exceptionMessage = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        String rootCauseMessage = "";
        
        // Get root cause message if available (this often contains the actual constraint name)
        if (ex.getRootCause() != null && ex.getRootCause().getMessage() != null) {
            rootCauseMessage = ex.getRootCause().getMessage().toLowerCase();
        }
        
        String combinedMessage = exceptionMessage + " " + rootCauseMessage;
        
        // Check for specific constraint violations based on actual database constraint names
        // PostgreSQL constraint names: users_email_key, users_phone_key, users_referral_code_key, etc.
        if (combinedMessage.contains("users_email_key") || 
            combinedMessage.contains("email") && combinedMessage.contains("unique") ||
            combinedMessage.contains("email") && combinedMessage.contains("duplicate") ||
            (combinedMessage.contains("constraint") && combinedMessage.contains("email") && 
             (combinedMessage.contains("users") || combinedMessage.contains("key")))) {
            errorMessage = "User with this email already exists";
        } else if (combinedMessage.contains("users_phone_key") || 
                   combinedMessage.contains("phone") && combinedMessage.contains("unique") ||
                   combinedMessage.contains("phone") && combinedMessage.contains("duplicate") ||
                   (combinedMessage.contains("constraint") && combinedMessage.contains("phone") && 
                    (combinedMessage.contains("users") || combinedMessage.contains("key")))) {
            errorMessage = "User with this phone number already exists";
        } else if (combinedMessage.contains("users_referral_code_key") || 
                   combinedMessage.contains("referral_code") && combinedMessage.contains("unique") ||
                   (combinedMessage.contains("constraint") && combinedMessage.contains("referral_code"))) {
            errorMessage = "Registration failed. Please try again."; // Referral code collision (very rare)
        } else if (combinedMessage.contains("unique") || 
                   combinedMessage.contains("duplicate") ||
                   combinedMessage.contains("_key") ||
                   combinedMessage.contains("unique constraint") ||
                   combinedMessage.contains("violates unique constraint")) {
            // Generic unique constraint violation
            errorMessage = "A record with this information already exists";
        } else if (combinedMessage.contains("foreign key") || 
                   combinedMessage.contains("fk_") ||
                   (combinedMessage.contains("references") && combinedMessage.contains("violates"))) {
            errorMessage = "Invalid reference. The provided data references a non-existent record.";
        }
        
        return build(HttpStatus.BAD_REQUEST, errorMessage, null, request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        // Security: Do not expose database error details to clients
        log.error("Database access error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", null, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied for request: {} - {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Access denied. You do not have permission to access this resource.", null, request);
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleInsufficientAuthentication(InsufficientAuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication required for request: {}", request.getRequestURI());
        return build(HttpStatus.UNAUTHORIZED, "Authentication required. Please provide a valid authentication token.", null, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Security: Do not leak internal details (stack traces, file paths, etc.) to clients
        // Log the full exception for debugging on server side
        log.error("=== GLOBAL EXCEPTION HANDLER TRIGGERED ===");
        log.error("Request path: {}", request.getRequestURI());
        log.error("Error class: {}", ex.getClass().getName());
        log.error("Error message: {}", ex.getMessage());
        log.error("Full stack trace:", ex);
        if (ex.getCause() != null) {
            log.error("Root cause: {} - {}", ex.getCause().getClass().getName(), ex.getCause().getMessage());
            if (ex.getCause().getCause() != null) {
                log.error("Root cause of root cause: {}", ex.getCause().getCause().getMessage());
            }
        }
        
        // For bundle endpoints, return 200 with empty data instead of 500
        String requestPath = request.getRequestURI();
        if (requestPath != null && requestPath.contains("/bundles")) {
            log.warn("Bundle endpoint error - returning 200 with empty data instead of 500");
            ListBundlesResponse emptyBundles = new ListBundlesResponse();
            emptyBundles.setBundles(new java.util.ArrayList<>());
            return ResponseEntity.ok(ApiResponse.success(emptyBundles, "No bundles available at this time"));
        }
        
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.", null, request);
    }

    private ResponseEntity<ApiResponse<Object>> build(HttpStatus status, String message, Object details, HttpServletRequest request) {
        Map<String, Object> err = new HashMap<>();
        err.put("status", status.value());
        err.put("error", status.getReasonPhrase());
        err.put("timestamp", OffsetDateTime.now().toString());
        err.put("path", request != null ? request.getRequestURI() : null);
        if (details != null) {
            err.put("details", details);
        }

        return ResponseEntity.status(status).body(ApiResponse.error(message != null ? message : status.getReasonPhrase(), err));
    }
    
    /**
     * Convert field error to response map.
     * Security: Do not expose rejected values that might contain sensitive data (passwords, tokens, etc.).
     */
    private Map<String, Object> toFieldError(FieldError fe) {
        Map<String, Object> m = new HashMap<>();
        m.put("field", fe.getField());
        
        // Security: Mask sensitive field values to prevent information leakage
        String fieldName = fe.getField().toLowerCase();
        if (fieldName.contains("password") || 
            fieldName.contains("secret") || 
            fieldName.contains("token") || 
            fieldName.contains("apikey") ||
            fieldName.contains("api_key") ||
            fieldName.contains("apisecret") ||
            fieldName.contains("api_secret")) {
            m.put("rejectedValue", "***REDACTED***");
        } else {
            m.put("rejectedValue", fe.getRejectedValue());
        }
        
        m.put("message", fe.getDefaultMessage());
        return m;
    }
}
