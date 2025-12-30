package com.tiktel.ttelgo.common.exception;

import com.tiktel.ttelgo.common.dto.ApiResponse;
import com.tiktel.ttelgo.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Handle BusinessException (our custom business logic exceptions)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        
        log.warn("Business exception [{}]: {} - Path: {}", 
                correlationId, ex.getMessage(), request.getRequestURI(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ex.getErrorCode().getCode())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .details(ex.getDetails())
                .build();
        
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), error);
        
        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(response);
    }
    
    /**
     * Handle ResourceNotFoundException
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        
        log.warn("Resource not found [{}]: {} - Path: {}", 
                correlationId, ex.getMessage(), request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ex.getErrorCode().getCode())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), error);
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }
    
    /**
     * Handle validation errors (@Valid, @Validated)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation error [{}]: {} errors - Path: {}", 
                correlationId, validationErrors.size(), request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.VALIDATION_ERROR.getCode())
                .message("Validation failed")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .details(validationErrors)
                .build();
        
        ApiResponse<Object> response = ApiResponse.error("Validation failed", error);
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    /**
     * Handle missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParams(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        
        log.warn("Missing parameter [{}]: {} - Path: {}", 
                correlationId, ex.getParameterName(), request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.INVALID_REQUEST.getCode())
                .message(String.format("Required parameter '%s' is missing", ex.getParameterName()))
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        ApiResponse<Object> response = ApiResponse.error(error.getMessage(), error);
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    /**
     * Handle type mismatch errors
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        
        log.warn("Type mismatch [{}]: {} - Path: {}", 
                correlationId, ex.getName(), request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.INVALID_REQUEST.getCode())
                .message(String.format("Invalid value for parameter '%s'", ex.getName()))
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        ApiResponse<Object> response = ApiResponse.error(error.getMessage(), error);
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    /**
     * Handle malformed JSON
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        
        log.warn("Malformed JSON [{}] - Path: {}", correlationId, request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.INVALID_REQUEST.getCode())
                .message("Malformed JSON request")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        ApiResponse<Object> response = ApiResponse.error(error.getMessage(), error);
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    
    /**
     * Handle authentication errors
     */
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(
            Exception ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        
        log.warn("Authentication failed [{}]: {} - Path: {}", 
                correlationId, ex.getMessage(), request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.UNAUTHORIZED.getCode())
                .message("Authentication failed")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        ApiResponse<Object> response = ApiResponse.error("Authentication failed", error);
        
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }
    
    /**
     * Handle authorization errors
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        
        log.warn("Access denied [{}]: {} - Path: {}", 
                correlationId, ex.getMessage(), request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.FORBIDDEN.getCode())
                .message("Access denied")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        ApiResponse<Object> response = ApiResponse.error("Access denied", error);
        
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }
    
    /**
     * Handle 404 Not Found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        
        log.warn("Endpoint not found [{}]: {} {} - Path: {}", 
                correlationId, ex.getHttpMethod(), ex.getRequestURL(), request.getRequestURI());
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.RESOURCE_NOT_FOUND.getCode())
                .message("Endpoint not found")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        ApiResponse<Object> response = ApiResponse.error("Endpoint not found", error);
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }
    
    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        
        log.error("Unexpected error [{}]: {} - Path: {}", 
                correlationId, ex.getMessage(), request.getRequestURI(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
                .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message("An unexpected error occurred")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
        
        ApiResponse<Object> response = ApiResponse.error("An unexpected error occurred", error);
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
    
    /**
     * Generate a unique correlation ID for tracking
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}

