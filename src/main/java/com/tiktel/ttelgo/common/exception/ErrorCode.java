package com.tiktel.ttelgo.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // General errors (1000-1099)
    INTERNAL_SERVER_ERROR("ERR_1000", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST("ERR_1001", "Invalid request", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("ERR_1002", "Resource not found", HttpStatus.NOT_FOUND),
    VALIDATION_ERROR("ERR_1003", "Validation error", HttpStatus.BAD_REQUEST),
    DUPLICATE_RESOURCE("ERR_1004", "Resource already exists", HttpStatus.CONFLICT),
    
    // Authentication & Authorization (1100-1199)
    UNAUTHORIZED("ERR_1100", "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("ERR_1101", "Forbidden", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS("ERR_1102", "Invalid credentials", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("ERR_1103", "Token expired", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("ERR_1104", "Invalid token", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED("ERR_1105", "Account locked", HttpStatus.FORBIDDEN),
    ACCOUNT_SUSPENDED("ERR_1106", "Account suspended", HttpStatus.FORBIDDEN),
    INSUFFICIENT_PERMISSIONS("ERR_1107", "Insufficient permissions", HttpStatus.FORBIDDEN),
    INVALID_API_KEY("ERR_1108", "Invalid API key", HttpStatus.UNAUTHORIZED),
    API_KEY_EXPIRED("ERR_1109", "API key expired", HttpStatus.UNAUTHORIZED),
    API_KEY_REVOKED("ERR_1110", "API key revoked", HttpStatus.UNAUTHORIZED),
    
    // Rate Limiting (1200-1299)
    RATE_LIMIT_EXCEEDED("ERR_1200", "Rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS),
    DAILY_LIMIT_EXCEEDED("ERR_1201", "Daily limit exceeded", HttpStatus.TOO_MANY_REQUESTS),
    MONTHLY_LIMIT_EXCEEDED("ERR_1202", "Monthly limit exceeded", HttpStatus.TOO_MANY_REQUESTS),
    
    // User errors (1300-1399)
    USER_NOT_FOUND("ERR_1300", "User not found", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("ERR_1301", "User already exists", HttpStatus.CONFLICT),
    EMAIL_NOT_VERIFIED("ERR_1302", "Email not verified", HttpStatus.FORBIDDEN),
    PHONE_NOT_VERIFIED("ERR_1303", "Phone not verified", HttpStatus.FORBIDDEN),
    INVALID_OTP("ERR_1304", "Invalid OTP", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED("ERR_1305", "OTP expired", HttpStatus.BAD_REQUEST),
    
    // Vendor errors (1400-1499)
    VENDOR_NOT_FOUND("ERR_1400", "Vendor not found", HttpStatus.NOT_FOUND),
    VENDOR_NOT_ACTIVE("ERR_1401", "Vendor not active", HttpStatus.FORBIDDEN),
    VENDOR_SUSPENDED("ERR_1402", "Vendor suspended", HttpStatus.FORBIDDEN),
    VENDOR_PENDING_APPROVAL("ERR_1403", "Vendor pending approval", HttpStatus.FORBIDDEN),
    INSUFFICIENT_WALLET_BALANCE("ERR_1404", "Insufficient wallet balance", HttpStatus.PAYMENT_REQUIRED),
    CREDIT_LIMIT_EXCEEDED("ERR_1405", "Credit limit exceeded", HttpStatus.PAYMENT_REQUIRED),
    VENDOR_DAILY_LIMIT_EXCEEDED("ERR_1406", "Vendor daily limit exceeded", HttpStatus.TOO_MANY_REQUESTS),
    VENDOR_MONTHLY_LIMIT_EXCEEDED("ERR_1407", "Vendor monthly limit exceeded", HttpStatus.TOO_MANY_REQUESTS),
    
    // Order errors (1500-1599)
    ORDER_NOT_FOUND("ERR_1500", "Order not found", HttpStatus.NOT_FOUND),
    ORDER_ALREADY_PAID("ERR_1501", "Order already paid", HttpStatus.CONFLICT),
    ORDER_ALREADY_CANCELED("ERR_1502", "Order already canceled", HttpStatus.CONFLICT),
    ORDER_CANNOT_BE_CANCELED("ERR_1503", "Order cannot be canceled", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_STATUS("ERR_1504", "Invalid order status", HttpStatus.BAD_REQUEST),
    ORDER_PROVISIONING_FAILED("ERR_1505", "Order provisioning failed", HttpStatus.INTERNAL_SERVER_ERROR),
    
    // Payment errors (1600-1699)
    PAYMENT_NOT_FOUND("ERR_1600", "Payment not found", HttpStatus.NOT_FOUND),
    PAYMENT_FAILED("ERR_1601", "Payment failed", HttpStatus.PAYMENT_REQUIRED),
    PAYMENT_ALREADY_PROCESSED("ERR_1602", "Payment already processed", HttpStatus.CONFLICT),
    INVALID_PAYMENT_AMOUNT("ERR_1603", "Invalid payment amount", HttpStatus.BAD_REQUEST),
    PAYMENT_METHOD_REQUIRED("ERR_1604", "Payment method required", HttpStatus.BAD_REQUEST),
    REFUND_FAILED("ERR_1605", "Refund failed", HttpStatus.INTERNAL_SERVER_ERROR),
    REFUND_AMOUNT_EXCEEDS_PAYMENT("ERR_1606", "Refund amount exceeds payment", HttpStatus.BAD_REQUEST),
    STRIPE_ERROR("ERR_1607", "Stripe payment error", HttpStatus.BAD_REQUEST),
    PAYMENT_INTENT_CREATION_FAILED("ERR_1608", "Payment intent creation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    
    // eSIM errors (1700-1799)
    ESIM_NOT_FOUND("ERR_1700", "eSIM not found", HttpStatus.NOT_FOUND),
    ESIM_ALREADY_ACTIVATED("ERR_1701", "eSIM already activated", HttpStatus.CONFLICT),
    ESIM_EXPIRED("ERR_1702", "eSIM expired", HttpStatus.BAD_REQUEST),
    ESIM_SUSPENDED("ERR_1703", "eSIM suspended", HttpStatus.BAD_REQUEST),
    INVALID_ICCID("ERR_1704", "Invalid ICCID", HttpStatus.BAD_REQUEST),
    QR_CODE_GENERATION_FAILED("ERR_1705", "QR code generation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    
    // Bundle/Catalogue errors (1800-1899)
    BUNDLE_NOT_FOUND("ERR_1800", "Bundle not found", HttpStatus.NOT_FOUND),
    BUNDLE_NOT_AVAILABLE("ERR_1801", "Bundle not available", HttpStatus.BAD_REQUEST),
    INVALID_BUNDLE_CODE("ERR_1802", "Invalid bundle code", HttpStatus.BAD_REQUEST),
    
    // eSIM Go Integration errors (1900-1999)
    ESIMGO_API_ERROR("ERR_1900", "eSIM Go API error", HttpStatus.BAD_GATEWAY),
    ESIMGO_TIMEOUT("ERR_1901", "eSIM Go API timeout", HttpStatus.GATEWAY_TIMEOUT),
    ESIMGO_UNAVAILABLE("ERR_1902", "eSIM Go service unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    ESIMGO_INVALID_RESPONSE("ERR_1903", "Invalid response from eSIM Go", HttpStatus.BAD_GATEWAY),
    ESIMGO_AUTHENTICATION_FAILED("ERR_1904", "eSIM Go authentication failed", HttpStatus.BAD_GATEWAY),
    ESIMGO_RATE_LIMIT("ERR_1905", "eSIM Go rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS),
    
    // Idempotency errors (2000-2099)
    IDEMPOTENCY_KEY_REQUIRED("ERR_2000", "Idempotency key required", HttpStatus.BAD_REQUEST),
    IDEMPOTENCY_KEY_MISMATCH("ERR_2001", "Idempotency key mismatch", HttpStatus.CONFLICT),
    DUPLICATE_REQUEST("ERR_2002", "Duplicate request", HttpStatus.CONFLICT),
    
    // Webhook errors (2100-2199)
    WEBHOOK_SIGNATURE_INVALID("ERR_2100", "Invalid webhook signature", HttpStatus.UNAUTHORIZED),
    WEBHOOK_PROCESSING_FAILED("ERR_2101", "Webhook processing failed", HttpStatus.INTERNAL_SERVER_ERROR),
    WEBHOOK_EVENT_NOT_FOUND("ERR_2102", "Webhook event not found", HttpStatus.NOT_FOUND),
    
    // Business logic errors (2200-2299)
    INSUFFICIENT_DATA("ERR_2200", "Insufficient data", HttpStatus.BAD_REQUEST),
    INVALID_COUNTRY_CODE("ERR_2201", "Invalid country code", HttpStatus.BAD_REQUEST),
    INVALID_CURRENCY("ERR_2202", "Invalid currency", HttpStatus.BAD_REQUEST),
    INVALID_QUANTITY("ERR_2203", "Invalid quantity", HttpStatus.BAD_REQUEST),
    OPERATION_NOT_ALLOWED("ERR_2204", "Operation not allowed", HttpStatus.FORBIDDEN),
    FRAUD_DETECTED("ERR_2205", "Suspicious activity detected", HttpStatus.FORBIDDEN),
    
    // Cache errors (2300-2399)
    CACHE_ERROR("ERR_2300", "Cache error", HttpStatus.INTERNAL_SERVER_ERROR),
    CACHE_MISS("ERR_2301", "Cache miss", HttpStatus.NOT_FOUND);
    
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
    
    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}

