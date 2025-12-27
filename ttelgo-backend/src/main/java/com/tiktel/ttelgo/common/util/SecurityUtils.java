package com.tiktel.ttelgo.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * Security utility class for input validation and sanitization following OWASP best practices.
 * 
 * This class provides static methods for:
 * - Input sanitization to prevent XSS
 * - Pattern validation for common input types
 * - SQL injection prevention (through parameterized queries - this is a helper)
 */
@Slf4j
public class SecurityUtils {
    
    // Patterns for validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$",
            Pattern.CASE_INSENSITIVE);
    
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");
    
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    
    private static final Pattern SAFE_STRING_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s._-]+$");
    
    // HTML/JavaScript characters that could be used for XSS
    private static final Pattern POTENTIALLY_DANGEROUS_PATTERN = Pattern.compile(
            "[<>\"'%();\\\\&+]|javascript:|on\\w+\\s*=", Pattern.CASE_INSENSITIVE);
    
    /**
     * Sanitize string input to prevent XSS attacks.
     * Removes or escapes potentially dangerous characters.
     * 
     * @param input The input string to sanitize
     * @return Sanitized string safe for output
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        // Replace potentially dangerous characters
        return input
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }
    
    /**
     * Check if input contains potentially dangerous patterns (XSS indicators).
     * 
     * @param input The input string to check
     * @return true if input appears safe, false if potentially dangerous
     */
    public static boolean isInputSafe(String input) {
        if (input == null) {
            return true;
        }
        return !POTENTIALLY_DANGEROUS_PATTERN.matcher(input).find();
    }
    
    /**
     * Validate email format.
     * 
     * @param email Email address to validate
     * @return true if valid email format
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    /**
     * Validate phone number format (E.164 format).
     * 
     * @param phone Phone number to validate
     * @return true if valid phone format
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }
    
    /**
     * Validate that string contains only alphanumeric characters.
     * 
     * @param input Input to validate
     * @return true if alphanumeric only
     */
    public static boolean isAlphanumeric(String input) {
        if (input == null) {
            return false;
        }
        return ALPHANUMERIC_PATTERN.matcher(input).matches();
    }
    
    /**
     * Validate that string contains only safe characters (alphanumeric, spaces, dots, underscores, hyphens).
     * 
     * @param input Input to validate
     * @return true if safe characters only
     */
    public static boolean isSafeString(String input) {
        if (input == null) {
            return false;
        }
        return SAFE_STRING_PATTERN.matcher(input).matches();
    }
    
    /**
     * Truncate string to maximum length to prevent buffer overflow or DoS.
     * 
     * @param input Input string
     * @param maxLength Maximum allowed length
     * @return Truncated string if needed
     */
    public static String truncate(String input, int maxLength) {
        if (input == null) {
            return null;
        }
        if (input.length() <= maxLength) {
            return input;
        }
        log.warn("Input truncated from {} to {} characters", input.length(), maxLength);
        return input.substring(0, maxLength);
    }
    
    /**
     * Normalize string input: trim whitespace and limit length.
     * 
     * @param input Input string
     * @param maxLength Maximum length
     * @return Normalized string
     */
    public static String normalize(String input, int maxLength) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        return truncate(trimmed, maxLength);
    }
    
    /**
     * Mask sensitive data for logging purposes.
     * 
     * @param sensitiveData The sensitive data to mask
     * @return Masked string (shows only last 4 characters if length > 4, otherwise all masked)
     */
    public static String maskSensitiveData(String sensitiveData) {
        if (sensitiveData == null || sensitiveData.isEmpty()) {
            return "***";
        }
        
        if (sensitiveData.length() <= 4) {
            return "***";
        }
        
        int visibleChars = 4;
        int maskedChars = sensitiveData.length() - visibleChars;
        return "*".repeat(Math.max(0, maskedChars)) + sensitiveData.substring(sensitiveData.length() - visibleChars);
    }
}

