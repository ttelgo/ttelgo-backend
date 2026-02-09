# Social Login Authentication - Complete Implementation

## Overview

This document describes the complete Spring Boot backend implementation for Social Login authentication supporting Google, Facebook, and Apple login for mobile applications.

## Tech Stack

- **Java 17**
- **Spring Boot 3.3.6**
- **Maven** project
- **REST APIs** only (no UI)
- **JWT-based authentication** for mobile apps
- **PostgreSQL** database

## API Endpoints

All social login endpoints accept a unified request format:

```json
{
  "idToken": "SOCIAL_PROVIDER_ID_TOKEN"
}
```

### 1. Google Sign-In

**Endpoint:** `POST /api/auth/google`

**Request Body:**
```json
{
  "idToken": "GOOGLE_ID_TOKEN"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "JWT_ACCESS_TOKEN",
    "refreshToken": "JWT_REFRESH_TOKEN",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "name": "John Doe",
      "firstName": "John",
      "lastName": "Doe",
      "phone": null,
      "isEmailVerified": true,
      "isPhoneVerified": false,
      "role": "USER"
    }
  }
}
```

**Token Verification:**
- Uses Google tokeninfo endpoint: `https://oauth2.googleapis.com/tokeninfo?id_token={idToken}`
- Validates email, name, and providerId (sub)
- Requires email_verified to be true

### 2. Facebook Sign-In

**Endpoint:** `POST /api/auth/facebook`

**Request Body:**
```json
{
  "idToken": "FACEBOOK_ACCESS_TOKEN"
}
```

**Response:** Same format as Google Sign-In

**Token Verification:**
- Uses Facebook Graph API: `https://graph.facebook.com/me?fields=id,name,email&access_token={token}`
- Validates user ID, name, and email (if provided)
- Handles cases where email is not provided by Facebook

### 3. Apple Sign-In

**Endpoint:** `POST /api/auth/apple`

**Request Body:**
```json
{
  "idToken": "APPLE_IDENTITY_TOKEN"
}
```

**Response:** Same format as Google Sign-In

**Token Verification:**
- Verifies JWT signature using Apple public keys (JWKS)
- Validates issuer: `https://appleid.apple.com`
- Validates audience (bundle id/client id)
- Handles first-time email provision (email may be null on subsequent logins)

## Implementation Details

### Project Structure

```
src/main/java/com/tiktel/ttelgo/
├── auth/
│   ├── api/
│   │   ├── SocialAuthController.java          # REST controller for /api/auth/*
│   │   └── dto/
│   │       └── SocialLoginRequest.java        # Unified request DTO
│   ├── application/
│   │   └── SocialAuthService.java             # Business logic for social login
│   └── infrastructure/
│       └── service/
│           ├── GoogleOAuthService.java         # Google token verification
│           ├── FacebookOAuthService.java        # Facebook token verification
│           └── AppleOAuthService.java          # Apple JWT verification
├── security/
│   ├── JwtTokenProvider.java                   # JWT token generation
│   └── SecurityConfig.java                     # Spring Security configuration
└── user/
    └── domain/
        └── User.java                           # User entity
```

### Key Components

#### 1. SocialAuthController

REST controller that handles all social login endpoints:
- `POST /api/auth/google`
- `POST /api/auth/facebook`
- `POST /api/auth/apple`

All endpoints accept `SocialLoginRequest` with `idToken` field.

#### 2. SocialAuthService

Service layer that implements the business logic:
- Token verification for each provider
- User lookup by email or providerId
- User creation if not exists
- JWT token generation
- Session management

#### 3. GoogleOAuthService

- **Primary Method:** `verifyIdTokenViaTokenInfo(String idToken)`
- Uses Google tokeninfo endpoint: `https://oauth2.googleapis.com/tokeninfo?id_token={idToken}`
- Extracts: email, name, sub (providerId), email_verified

#### 4. FacebookOAuthService

- **Primary Method:** `verifyAccessToken(String accessToken)`
- Uses Facebook Graph API: `https://graph.facebook.com/me?fields=id,name,email&access_token={token}`
- Extracts: id (providerId), name, email, first_name, last_name

#### 5. AppleOAuthService

- **Primary Method:** `verifyIdentityToken(String identityToken)`
- Fetches Apple public keys from JWKS endpoint
- Validates JWT signature, issuer, audience, expiration
- Caches public keys for 1 hour
- Extracts: email, sub (providerId), name (first_name, last_name)

### Database Schema

The `users` table includes:
- `id` (Primary Key)
- `email` (Unique)
- `name`
- `provider` (GOOGLE, FACEBOOK, APPLE, LOCAL, EMAIL)
- `provider_id` (Social provider user ID)
- `first_name`
- `last_name`
- `is_email_verified`
- `created_at`
- `updated_at`

### Security Configuration

The `/api/auth/**` endpoints are configured as public (no authentication required) in `SecurityConfig.java`:

```java
.requestMatchers("/api/auth/**").permitAll()
```

### JWT Token Generation

After successful social login:
1. **Access Token**: Generated with user information, expires in 24 hours (configurable)
2. **Refresh Token**: Generated for token refresh, expires in 7 days
3. **Session**: Created in database to track active sessions

## Configuration

### application.yml

```yaml
# Google OAuth Configuration
google:
  oauth:
    client-id: ${GOOGLE_OAUTH_CLIENT_ID:}

# Apple OAuth Configuration
apple:
  oauth:
    client-id: ${APPLE_OAUTH_CLIENT_ID:}

# Facebook OAuth Configuration
facebook:
  oauth:
    app-id: ${FACEBOOK_OAUTH_APP_ID:}
    app-secret: ${FACEBOOK_OAUTH_APP_SECRET:}

# JWT Configuration
jwt:
  secret: ${JWT_SECRET:}
  expiration: ${JWT_EXPIRATION:86400000} # 24 hours
  refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800000} # 7 days
```

### Environment Variables

Set the following environment variables:

```bash
# Google OAuth
GOOGLE_OAUTH_CLIENT_ID=your-google-client-id.apps.googleusercontent.com

# Apple OAuth
APPLE_OAUTH_CLIENT_ID=com.yourcompany.yourapp

# Facebook OAuth
FACEBOOK_OAUTH_APP_ID=your-facebook-app-id
FACEBOOK_OAUTH_APP_SECRET=your-facebook-app-secret

# JWT
JWT_SECRET=your-jwt-secret-key-min-256-bits
```

## Testing with Postman

### 1. Google Sign-In

**Request:**
```
POST http://localhost:8080/api/auth/google
Content-Type: application/json

{
  "idToken": "YOUR_GOOGLE_ID_TOKEN"
}
```

### 2. Facebook Sign-In

**Request:**
```
POST http://localhost:8080/api/auth/facebook
Content-Type: application/json

{
  "idToken": "YOUR_FACEBOOK_ACCESS_TOKEN"
}
```

### 3. Apple Sign-In

**Request:**
```
POST http://localhost:8080/api/auth/apple
Content-Type: application/json

{
  "idToken": "YOUR_APPLE_IDENTITY_TOKEN"
}
```

## Error Handling

All endpoints return standardized error responses:

```json
{
  "success": false,
  "message": "Error message",
  "errors": {
    "status": 401,
    "error": "Unauthorized",
    "timestamp": "2025-01-01T12:00:00Z",
    "path": "/api/auth/google"
  }
}
```

Common error codes:
- `400 Bad Request`: Invalid request format
- `401 Unauthorized`: Invalid or expired social token
- `500 Internal Server Error`: Server error

## User Creation Logic

1. **First Login (New User):**
   - User is created with provider (GOOGLE/FACEBOOK/APPLE)
   - providerId is stored
   - Email is set (or placeholder if not provided)
   - Referral code is generated
   - Role is set to USER
   - UserType is set to CUSTOMER

2. **Subsequent Login (Existing User):**
   - User is found by email or providerId
   - Provider information is updated if missing
   - Name and other fields are updated if available

## Security Best Practices

1. **Token Verification:**
   - All social tokens are verified before user creation/login
   - Google: email_verified must be true
   - Facebook: Token validated via Graph API
   - Apple: JWT signature, issuer, and audience validated

2. **JWT Security:**
   - Tokens are signed with a secret key
   - Access tokens expire in 24 hours
   - Refresh tokens expire in 7 days
   - Sessions are tracked in database

3. **Database:**
   - User emails are normalized (lowercase)
   - Provider IDs are stored for reliable lookup
   - Referral codes are unique

## Dependencies

Key Maven dependencies:

```xml
<!-- Google OAuth -->
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version>2.2.0</version>
</dependency>

<!-- Apple Sign-In -->
<dependency>
    <groupId>com.nimbusds</groupId>
    <artifactId>nimbus-jose-jwt</artifactId>
    <version>9.37.3</version>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
```

## Notes

1. **Google Token Verification:**
   - Uses tokeninfo endpoint as specified in requirements
   - Alternative: GoogleIdTokenVerifier (also available but not used in /api/auth/*)

2. **Facebook Email Handling:**
   - Facebook may not always provide email
   - Placeholder email format: `{providerId}@facebook.private`
   - Email verification is set to false if email not provided

3. **Apple Email Handling:**
   - Email is only provided on first login
   - Subsequent logins use providerId for lookup
   - Placeholder email format: `{providerId}@apple.private`

4. **Session Management:**
   - Sessions are created for all social logins
   - Sessions track access tokens and refresh tokens
   - Sessions can be invalidated on logout

## Production Checklist

- [ ] Set all OAuth client IDs and secrets via environment variables
- [ ] Configure JWT secret (minimum 256 bits)
- [ ] Enable HTTPS in production
- [ ] Configure CORS for your mobile app domains
- [ ] Set up database connection pooling
- [ ] Configure Redis for session caching (optional)
- [ ] Set up monitoring and logging
- [ ] Review and test all error scenarios
- [ ] Implement rate limiting for auth endpoints
- [ ] Set up backup and recovery procedures

## Support

For issues or questions, refer to:
- Spring Boot Documentation: https://spring.io/projects/spring-boot
- Google OAuth: https://developers.google.com/identity/protocols/oauth2
- Facebook Login: https://developers.facebook.com/docs/facebook-login
- Apple Sign-In: https://developer.apple.com/sign-in-with-apple/

