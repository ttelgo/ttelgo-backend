# Security Implementation (R10) - OWASP Best Practices

This document outlines the security measures implemented to follow OWASP best practices and secure coding standards.

## Overview

Implementation date: Current
Status: ✅ Complete

## Implemented Security Measures

### 1. Security HTTP Headers

**Location:** `com.tiktel.ttelgo.security.SecurityHeadersFilter`

All HTTP responses now include security headers to prevent common attacks:

- **X-Content-Type-Options: nosniff** - Prevents MIME type sniffing
- **X-Frame-Options: DENY** - Prevents clickjacking attacks
- **X-XSS-Protection: 1; mode=block** - Enables XSS protection (legacy browsers)
- **Strict-Transport-Security** - Enforces HTTPS (HSTS) for secure connections
- **Content-Security-Policy** - Restricts resource loading to prevent XSS
- **Referrer-Policy: strict-origin-when-cross-origin** - Controls referrer information
- **Permissions-Policy** - Controls browser features (camera, microphone, etc.)

**Implementation Details:**
- Filter runs early in the filter chain (Order 1)
- Automatically applied to all requests
- HSTS header only added for HTTPS connections

### 2. Secure Error Handling

**Location:** `com.tiktel.ttelgo.common.exception.GlobalExceptionHandler`

Enhanced exception handling to prevent information leakage:

- **No Stack Traces in Responses** - Generic error messages returned to clients
- **Database Error Masking** - Database constraint violations and errors are logged internally but not exposed to clients
- **Sensitive Data Masking in Validation Errors** - Field values containing passwords, secrets, tokens, or API keys are redacted (showing "***REDACTED***" instead)
- **Detailed Server-Side Logging** - Full exception details logged server-side for debugging

**Exception Handlers:**
- `DataIntegrityViolationException` - Returns generic "Invalid data provided"
- `DataAccessException` - Returns generic "Internal Server Error"
- `MethodArgumentNotValidException` - Masks sensitive field values
- `MaxUploadSizeExceededException` - Handles oversized requests
- `HttpRequestMethodNotSupportedException` - Proper 405 responses

### 3. Input Validation and Sanitization

**Location:** Multiple DTOs and `com.tiktel.ttelgo.common.util.SecurityUtils`

Enhanced input validation across the application:

#### DTO-Level Validation
- `OtpRequest` - Email and phone number format validation, purpose enum validation
- `CreateApiKeyRequest` - String length limits, email validation, numeric ranges

#### Security Utilities (`SecurityUtils`)
Provides static methods for:
- **Input Sanitization** - Escapes HTML/JavaScript characters to prevent XSS
- **Pattern Validation** - Email, phone number, alphanumeric patterns
- **Safe String Validation** - Validates strings contain only safe characters
- **Input Truncation** - Prevents buffer overflow and DoS attacks
- **Sensitive Data Masking** - Utility for masking sensitive data in logs

**Key Methods:**
```java
- sanitizeInput(String) - Escapes dangerous characters
- isInputSafe(String) - Checks for XSS indicators
- isValidEmail(String) - Validates email format
- isValidPhone(String) - Validates phone format (E.164)
- truncate(String, int) - Limits string length
- maskSensitiveData(String) - Masks for logging
```

### 4. Request Size Limits

**Location:** `application.yml`

Configured limits to prevent DoS attacks:

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
  http:
    max-header-size: 8KB

server:
  max-http-header-size: 8KB
```

- Maximum file upload size: 10MB
- Maximum request size: 10MB
- Maximum HTTP header size: 8KB

### 5. Dependency Vulnerability Scanning

**Location:** `pom.xml`

OWASP Dependency-Check Maven plugin integrated:

```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>9.0.9</version>
</plugin>
```

**Usage:**
```bash
mvn dependency-check:check
```

- Scans all dependencies for known vulnerabilities
- Generates HTML and JSON reports
- Fails build on CVSS score ≥ 7.0 (configurable)
- Skips test scope by default for faster scans

### 6. Secure Configuration

**Existing Security Measures (Already in Place):**

- ✅ **JWT Authentication** - Secure token-based authentication
- ✅ **API Key Authentication** - Secure API key-based authentication
- ✅ **Role-Based Access Control (RBAC)** - Fine-grained permissions
- ✅ **Password Hashing** - BCrypt password encoding
- ✅ **HTTPS Enforcement** - Configured via security headers (HSTS)
- ✅ **CORS Configuration** - Restricted allowed origins
- ✅ **Stateless Architecture** - No session-based vulnerabilities
- ✅ **SQL Injection Prevention** - JPA/Hibernate with parameterized queries

## Security Best Practices Followed

### OWASP Top 10 (2021) Coverage

1. **A01: Broken Access Control** ✅
   - JWT and API key authentication
   - Role-based access control
   - Method-level security annotations

2. **A02: Cryptographic Failures** ✅
   - BCrypt password hashing
   - HTTPS enforcement (HSTS)
   - Sensitive data masking in logs

3. **A03: Injection** ✅
   - JPA/Hibernate parameterized queries (SQL injection prevention)
   - Input validation and sanitization (XSS prevention)
   - Bean Validation annotations

4. **A04: Insecure Design** ✅
   - Security headers
   - Secure error handling
   - Request size limits

5. **A05: Security Misconfiguration** ✅
   - Security headers configured
   - Proper error handling
   - Dependency vulnerability scanning

6. **A06: Vulnerable and Outdated Components** ✅
   - OWASP Dependency-Check integrated
   - Regular dependency updates

7. **A07: Identification and Authentication Failures** ✅
   - JWT with refresh tokens
   - API key authentication
   - Secure password storage

8. **A08: Software and Data Integrity Failures** ✅
   - Input validation
   - Secure error handling
   - Request size limits

9. **A09: Security Logging and Monitoring Failures** ✅
   - Comprehensive exception logging
   - Sensitive data masking in logs
   - Audit-ready error handling

10. **A10: Server-Side Request Forgery (SSRF)** ✅
    - Input validation
    - Restricted URL patterns
    - External API integration with validation

## Testing Recommendations

1. **Security Headers Testing:**
   - Use browser dev tools to verify headers are present
   - Test with security header scanning tools (e.g., securityheaders.com)

2. **Input Validation Testing:**
   - Test XSS payloads in input fields
   - Test SQL injection attempts (should be blocked by JPA)
   - Test oversized requests (should return 413)

3. **Error Handling Testing:**
   - Trigger various exceptions and verify no sensitive data is leaked
   - Verify validation errors mask sensitive fields

4. **Dependency Scanning:**
   - Run `mvn dependency-check:check` regularly
   - Review and update vulnerable dependencies

## Maintenance

### Regular Tasks

1. **Monthly:**
   - Run dependency vulnerability scan
   - Review and update vulnerable dependencies
   - Review security logs

2. **Quarterly:**
   - Review and update security headers policies
   - Review input validation rules
   - Security audit of error handling

3. **On Security Advisories:**
   - Immediately run dependency check
   - Update affected dependencies
   - Test critical paths

## References

- [OWASP Top 10 (2021)](https://owasp.org/www-project-top-ten/)
- [OWASP Secure Coding Practices](https://owasp.org/www-project-secure-coding-practices-quick-reference-guide/)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/index.html)
- [OWASP Dependency-Check](https://owasp.org/www-project-dependency-check/)

