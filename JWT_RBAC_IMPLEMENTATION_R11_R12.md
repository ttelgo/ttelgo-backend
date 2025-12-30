# JWT Authentication & RBAC Implementation (R11 & R12)

This document outlines the complete implementation of secure JWT authentication with refresh tokens and Role-Based Access Control (RBAC).

## Overview

Implementation date: Current
Status: ✅ Complete

## R11: Secure Authentication with JWT/OAuth2 and Refresh Tokens

### 1. JWT Token Provider with Roles

**Location:** `com.tiktel.ttelgo.security.JwtTokenProvider`

Enhanced JWT token generation to include user roles:

- **Access Tokens**: Include user ID, email, role, and token type ("access")
- **Refresh Tokens**: Include user ID, email, role, and token type ("refresh")
- **Token Validation**: Validates token signature, expiration, and structure
- **Role Extraction**: Methods to extract role and token type from tokens

**Key Methods:**
```java
generateToken(Long userId, String email, String role) // Generate access token with role
generateRefreshToken(Long userId, String email, String role) // Generate refresh token with role
getRoleFromToken(String token) // Extract role from token
getTokenTypeFromToken(String token) // Extract token type (access/refresh)
validateToken(String token) // Validate token
```

### 2. UserPrincipal for Spring Security

**Location:** `com.tiktel.ttelgo.security.UserPrincipal`

Implements Spring Security's `UserDetails` interface:

- Maps `User` entity to Spring Security user details
- Converts `UserRole` enum (USER, ADMIN, SUPER_ADMIN) to Spring Security authorities (ROLE_USER, ROLE_ADMIN, ROLE_SUPER_ADMIN)
- Provides user authentication information to Spring Security context

### 3. Custom UserDetailsService

**Location:** `com.tiktel.ttelgo.security.CustomUserDetailsService`

Service to load user details for authentication:

- `loadUserByUsername(String email)`: Loads user by email for login
- `loadUserById(Long userId)`: Loads user by ID (used when validating JWT tokens)
- Returns `UserPrincipal` with user roles and authorities

### 4. JWT Authentication Filter

**Location:** `com.tiktel.ttelgo.security.JwtAuthenticationFilter`

Servlet filter that intercepts HTTP requests and validates JWT tokens:

- Extracts JWT token from `Authorization: Bearer <token>` header
- Validates token signature and expiration
- Ensures only access tokens (not refresh tokens) are used for authentication
- Loads user details and sets authentication in Spring Security context
- Allows request to proceed if authentication is successful

**Process:**
1. Extract token from Authorization header
2. Validate token
3. Extract user ID from token
4. Load user details via `CustomUserDetailsService`
5. Create Spring Security authentication object with user authorities (roles)
6. Set authentication in Spring Security context

### 5. Token Generation in AuthService

**Location:** `com.tiktel.ttelgo.auth.application.AuthService`

Updated to include roles when generating tokens:

- When user verifies OTP or logs in, tokens include user's role
- When refresh token is used, new tokens include current user's role
- Ensures role changes are reflected in new tokens

### 6. Configuration

**Location:** `application.yml`

JWT configuration with environment variable support:

```yaml
jwt:
  secret: ${JWT_SECRET:default-secret-for-dev}
  expiration: ${JWT_EXPIRATION:86400000} # 24 hours
  refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800000} # 7 days
```

**Security Note:** In production, `JWT_SECRET` must be:
- At least 256 bits (32 bytes)
- Stored as environment variable
- Randomly generated and kept secure
- Never committed to version control

## R12: Role-Based Access Control (RBAC)

### 1. User Roles

**Location:** `com.tiktel.ttelgo.user.domain.User.UserRole`

Three role levels:
- **USER**: Standard user (default)
- **ADMIN**: Administrative user with elevated permissions
- **SUPER_ADMIN**: Super administrator with full system access

### 2. Method-Level Security with @PreAuthorize

All admin endpoints are protected with `@PreAuthorize` annotations:

**Protected Controllers:**
- `AdminController` - Dashboard statistics
- `AdminUserController` - User management
- `AdminOrderController` - Order management
- `AdminPlanController` - Plan/bundle management
- `AdminEsimController` - eSIM management
- `AdminPostController` - Blog post management
- `AdminFaqController` - FAQ management
- `ApiKeyController` - API key management

**Annotation Pattern:**
```java
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
```

This ensures only users with ADMIN or SUPER_ADMIN roles can access these endpoints.

### 3. Spring Security Configuration

**Location:** `com.tiktel.ttelgo.security.SecurityConfig`

- `@EnableMethodSecurity`: Enables method-level security annotations
- JWT Authentication Filter integrated into filter chain
- Admin endpoints require authentication (enforced via `@PreAuthorize`)

**Filter Chain Order:**
1. SecurityHeadersFilter - Adds security HTTP headers
2. ApiKeyAuthenticationFilter - Validates API keys (for external integrations)
3. JwtAuthenticationFilter - Validates JWT tokens (for user authentication)
4. IdempotencyFilter - Handles idempotency keys

### 4. Access Control Flow

1. **User Authentication:**
   - User logs in via `/api/v1/auth/otp/verify`
   - Receives access token and refresh token (both include user role)

2. **API Request:**
   - Client sends request with `Authorization: Bearer <access_token>` header
   - `JwtAuthenticationFilter` extracts and validates token
   - User details loaded with roles and authorities
   - Authentication set in Spring Security context

3. **Authorization Check:**
   - Spring Security evaluates `@PreAuthorize` annotation
   - Checks if user has required role (ADMIN or SUPER_ADMIN)
   - Allows or denies access accordingly

4. **Token Refresh:**
   - Client can refresh access token using refresh token via `/api/v1/auth/refresh`
   - New tokens generated with current user role

## Security Features

### 1. Secure Token Storage
- Tokens stored in database (`sessions` table)
- Sessions tracked with expiration times
- Logout invalidates tokens

### 2. Token Expiration
- Access tokens: 24 hours (configurable)
- Refresh tokens: 7 days (configurable)
- Expired tokens automatically rejected

### 3. Role-Based Authorization
- Fine-grained control via method-level annotations
- Multiple roles can access same endpoint if needed
- Easy to extend with additional permissions

### 4. Stateless Authentication
- JWT tokens contain all necessary information
- No server-side session storage required
- Scalable for distributed systems

## API Endpoints

### Authentication Endpoints

- `POST /api/v1/auth/otp/request` - Request OTP (public)
- `POST /api/v1/auth/otp/verify` - Verify OTP and get tokens (public)
- `POST /api/v1/auth/register` - Register new user (public)
- `POST /api/v1/auth/refresh` - Refresh access token (public)
- `POST /api/v1/auth/logout` - Logout and invalidate tokens (authenticated)

### Admin Endpoints (Require ADMIN or SUPER_ADMIN role)

- `GET /api/v1/admin/dashboard` - Dashboard statistics
- `GET /api/v1/admin/users` - List all users
- `GET /api/v1/admin/users/{id}` - Get user by ID
- `PUT /api/v1/admin/users/{id}` - Update user
- `DELETE /api/v1/admin/users/{id}` - Delete user
- `GET /api/v1/admin/orders` - List all orders
- `GET /api/v1/admin/bundles` - List all bundles
- `GET /api/v1/admin/esims` - List all eSIMs
- `GET /api/v1/admin/posts` - List all blog posts
- `GET /api/v1/admin/faqs` - List all FAQs
- `GET /api/v1/admin/api-keys` - List all API keys

## Usage Examples

### 1. Login and Get Tokens

```bash
POST /api/v1/auth/otp/verify
{
  "email": "user@example.com",
  "otp": "123456"
}

Response:
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "email": "user@example.com",
      ...
    }
  }
}
```

### 2. Access Protected Endpoint

```bash
GET /api/v1/admin/users
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

Response (if user has ADMIN/SUPER_ADMIN role):
{
  "success": true,
  "data": [...],
  "meta": {...}
}

Response (if user doesn't have required role):
403 Forbidden
```

### 3. Refresh Token

```bash
POST /api/v1/auth/refresh
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}

Response:
{
  "success": true,
  "data": {
    "token": "new_access_token...",
    "refreshToken": "new_refresh_token...",
    ...
  }
}
```

## Testing

### 1. Test JWT Authentication
- Login and receive tokens
- Use access token in Authorization header
- Verify user can access endpoints based on role

### 2. Test RBAC
- Login as USER role - should NOT access admin endpoints (403)
- Login as ADMIN role - should access admin endpoints (200)
- Login as SUPER_ADMIN role - should access admin endpoints (200)

### 3. Test Token Refresh
- Use refresh token to get new access token
- Verify new token works for authentication
- Verify expired refresh token is rejected

## Configuration for Production

1. **Set JWT_SECRET environment variable:**
   ```bash
   export JWT_SECRET=$(openssl rand -base64 32)
   ```

2. **Update application-prod.yml:**
   ```yaml
   jwt:
     secret: ${JWT_SECRET}
     expiration: 3600000  # 1 hour for production
     refresh-expiration: 604800000  # 7 days
   ```

3. **Review role assignments:**
   - Ensure only trusted users have ADMIN or SUPER_ADMIN roles
   - Regularly audit role assignments

## Future Enhancements

1. **Permissions (Fine-grained access control):**
   - Add permission-based access control
   - Example: `@PreAuthorize("hasPermission('user', 'delete')")`

2. **Token Revocation:**
   - Implement token blacklist
   - Support for revoking tokens before expiration

3. **Multi-factor Authentication (MFA):**
   - Add MFA support for enhanced security

4. **OAuth2 Integration:**
   - Support for OAuth2 providers (Google, Facebook, etc.)

## References

- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/index.html)
- [JWT.io](https://jwt.io/) - JWT token decoder and debugger
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)

