# How to Check if User is Logged In - Postman Guide

## Quick Answer

**Use this endpoint to check if you're logged in:**
```
GET http://localhost:8080/api/v1/users/me
```

---

## Step-by-Step Instructions

### Step 1: Get Your JWT Token

After verifying OTP, you should have received a response like:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900000,
    "user": {
      "id": 1,
      "email": "abdulqayumsabir41@gmail.com",
      ...
    }
  }
}
```

**Copy the `accessToken` value.**

---

### Step 2: Set Authorization Header in Postman

1. **Create a new request** in Postman:
   - Method: `GET`
   - URL: `http://localhost:8080/api/v1/users/me`

2. **Go to the "Headers" tab**

3. **Add Authorization header:**
   - **Key:** `Authorization`
   - **Value:** `Bearer <paste_your_accessToken_here>`
   
   Example:
   ```
   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsImVtYWlsIjoiYWJkdWxxYXl1bXNhYmlyNDFAZ21haWwuY29tIn0...
   ```

4. **Click "Send"**

---

### Step 3: Check the Response

#### ✅ **If You're Logged In (Success):**

**Status:** `200 OK`

**Response:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 1,
    "email": "abdulqayumsabir41@gmail.com",
    "name": "Abdul Qayum",
    "phone": null,
    "firstName": "Abdul",
    "lastName": "Qayum",
    "isEmailVerified": true,
    "isPhoneVerified": false,
    "role": "USER"
  },
  "timestamp": "2026-01-27T16:00:00Z"
}
```

**This means you're logged in!** ✅

---

#### ❌ **If You're NOT Logged In (Error):**

**Status:** `401 Unauthorized`

**Response:**
```json
{
  "success": false,
  "message": "Authentication required. Please provide a valid authentication token.",
  "error": {
    "code": "ERR_1100",
    "message": "Unauthorized"
  },
  "timestamp": "2026-01-27T16:00:00Z"
}
```

**This means you're not logged in or the token is invalid/expired.** ❌

---

## Using Postman Environment Variables (Recommended)

### Setup:

1. **Create/Edit Environment** in Postman
2. **Add variable:**
   - Variable: `accessToken`
   - Initial Value: (leave empty)
   - Current Value: (leave empty)

3. **After OTP verification:**
   - Copy the `accessToken` from response
   - Set it in your environment: `accessToken = <your_token>`

4. **In your requests, use:**
   - Header Key: `Authorization`
   - Header Value: `Bearer {{accessToken}}`

---

## Alternative: Check Token Validity

You can also test any protected endpoint. If it works, you're logged in!

**Test Endpoints:**
- `GET /api/v1/users/me` - Get current user info
- `GET /api/v1/orders` - Get user orders (if you have any)
- `GET /api/v1/esims` - Get user eSIMs (if you have any)

All of these require authentication, so if they return `200 OK`, you're logged in!

---

## Troubleshooting

### Problem: Getting 401 Unauthorized

**Solutions:**
1. ✅ Check that you're using `Bearer ` (with space) before the token
2. ✅ Make sure the token hasn't expired (check `expiresIn` in response)
3. ✅ Copy the entire token (it's very long)
4. ✅ Try requesting a new OTP and getting a fresh token

### Problem: Token Expired

**Solution:** Request a new OTP and verify again to get a new token.

---

## Summary

**To check if logged in:**
1. Use the `accessToken` from OTP verification response
2. Add `Authorization: Bearer <token>` header
3. Call `GET /api/v1/users/me`
4. If you get user data → **You're logged in!** ✅
5. If you get 401 error → **Not logged in** ❌

