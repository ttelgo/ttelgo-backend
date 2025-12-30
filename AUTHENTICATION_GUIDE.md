# TTelGo Backend - Authentication Guide

## Overview

The TTelGo backend uses **JWT (JSON Web Token)** authentication for securing API endpoints. This guide explains how to obtain and use JWT tokens for API testing.

---

## 🔐 Security Model

### Public Endpoints (No Authentication Required)
- ✅ `/api/auth/**` - Authentication endpoints
- ✅ `/api/webhooks/stripe/**` - Stripe webhooks
- ✅ `/actuator/health/**` - Health checks
- ✅ `/swagger-ui/**` - API documentation

### Protected Endpoints (Authentication Required)
- 🔒 `/api/v1/catalogue/**` - eSIM Bundles
- 🔒 `/api/v1/orders/**` - Order management
- 🔒 `/api/v1/esims/**` - eSIM lifecycle
- 🔒 `/api/v1/payments/**` - Payment processing
- 🔒 `/api/v1/vendor/**` - Vendor APIs
- 🔒 All other endpoints

---

## 📝 Getting a JWT Token (For Testing)

### Step 1: Generate a Test Token

**Endpoint:** `POST http://localhost:8080/api/auth/test/token`

**Request Body:**
```json
{
  "userId": 1,
  "email": "test@ttelgo.com"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": 1,
  "email": "test@ttelgo.com"
}
```

### Step 2: Use the Token in API Requests

Add the `Authorization` header to your requests:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 🧪 Testing with Postman

### Option 1: Manual Header

1. Copy the `accessToken` from the response
2. In Postman, go to the **Headers** tab
3. Add a new header:
   - **Key:** `Authorization`
   - **Value:** `Bearer <paste_your_token_here>`

### Option 2: Environment Variable (Recommended)

1. **Generate Token:**
   - Send POST request to `/api/auth/test/token`
   - Copy the `accessToken` from response

2. **Set Environment Variable:**
   - Go to **Environments** in Postman
   - Create/Edit `TTelGo Local` environment
   - Add variable:
     - **Variable:** `jwt_token`
     - **Initial Value:** (paste your token)
     - **Current Value:** (paste your token)

3. **Use in Requests:**
   - In the **Authorization** tab
   - Select type: **Bearer Token**
   - Token: `{{jwt_token}}`

### Option 3: Auto-Extract Token (Advanced)

Add this to the **Tests** tab of the token generation request:

```javascript
// Extract and save the access token
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.environment.set("jwt_token", jsonData.accessToken);
    console.log("JWT Token saved to environment!");
}
```

Now the token will automatically be saved to your environment variable!

---

## ✅ Validating Your Token

**Endpoint:** `GET http://localhost:8080/api/auth/test/validate?token=<your_token>`

**Response:**
```json
{
  "valid": true,
  "message": "Token is valid",
  "userId": 1,
  "email": "test@ttelgo.com",
  "expired": false
}
```

---

## 🔄 Token Lifecycle

| Token Type | Expiration | Purpose |
|------------|------------|---------|
| **Access Token** | 24 hours | Used for API requests |
| **Refresh Token** | 7 days | Used to get new access tokens |

### Token Expiration

When your access token expires (after 24 hours), you'll get a **401 Unauthorized** response. Simply generate a new token using the `/api/auth/test/token` endpoint.

---

## 📋 Example API Calls with Authentication

### Example 1: List eSIM Bundles

```bash
curl -X GET "http://localhost:8080/api/v1/catalogue/bundles?page=0&size=20" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Example 2: Create Order

```bash
curl -X POST "http://localhost:8080/api/v1/orders" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: order-12345" \
  -d '{
    "bundleCode": "BUNDLE-US-1GB",
    "quantity": 1,
    "currency": "USD",
    "customerEmail": "customer@example.com"
  }'
```

---

## 🚨 Important Notes

### ⚠️ Test Endpoint Security Warning

The `/api/auth/test/token` endpoint is **FOR TESTING ONLY** and should be:
- ✅ Used in **development** and **testing** environments
- ❌ **REMOVED or DISABLED** in **production**
- ❌ **NEVER exposed** to the public internet in production

### 🔒 Production Authentication

In production, use proper authentication mechanisms:
- User registration and login with password
- Email/phone verification
- Role-based access control (RBAC)
- API keys for B2B vendor access
- OAuth2/OpenID Connect for third-party integrations

---

## 🐛 Troubleshooting

### Issue: 401 Unauthorized

**Possible Causes:**
1. Token is expired
2. Token is malformed
3. Authorization header is missing
4. Token format is incorrect (should be `Bearer <token>`)

**Solution:**
1. Generate a new token
2. Check token format in Authorization header
3. Ensure `Bearer ` prefix is included (with space)

### Issue: 403 Forbidden

**Possible Causes:**
1. Token is valid but user doesn't have permission
2. Endpoint requires specific roles

**Solution:**
1. Check if your user has the required role
2. Verify endpoint access requirements

---

## 📚 Additional Resources

- [JWT.io](https://jwt.io/) - Decode and inspect JWT tokens
- [Postman JWT Auth Documentation](https://learning.postman.com/docs/sending-requests/authorization/#bearer-token)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)

---

## 🎯 Quick Start Checklist

- [ ] Start the application
- [ ] Generate a test JWT token using `/api/auth/test/token`
- [ ] Save the token to Postman environment variable
- [ ] Test a protected endpoint (e.g., `/api/v1/catalogue/bundles`)
- [ ] Verify authentication is working correctly

---

**Happy Testing! 🚀**

