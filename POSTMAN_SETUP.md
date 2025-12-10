# Postman Collection Setup Guide

## Importing the Collection

1. **Open Postman**
2. **Drag and drop** `TTelGo_API_Collection_AutoDetect.postman_collection.json` into Postman window
   - OR click **Import** button → Select **File** → Choose the JSON file
3. Postman will auto-detect it as a collection
4. Click **Import**
5. The collection will appear in Collections sidebar with all 9 folders and 39 requests

## Environment Variables

The collection uses the following variables:

### Required Variables

1. **`baseUrl`** - Base URL for the API
   - **Local Development:** `http://localhost:8080`
   - **Production:** `https://www.ttelgo.com`

2. **`accessToken`** - JWT access token (automatically set after login)
   - Get this from the "Verify OTP" response
   - Used in Authorization header for protected endpoints

### Setting Up Environment Variables

1. In Postman, click **Environments** (left sidebar)
2. Create a new environment:
   - **Name:** `TTelGo Local` or `TTelGo Production`
   - **Variables:**
     - `baseUrl`: `http://localhost:8080` (or production URL)
     - `accessToken`: (leave empty, will be set after login)
3. Select the environment from the dropdown (top right)

## Quick Start Workflow

### 1. Get Authentication Token

1. Go to **Authentication** folder
2. Run **"Request OTP"** with your email/phone
3. Check your email/SMS for OTP code
4. Run **"Verify OTP"** with the OTP code
5. Copy the `token` from response
6. Set it as `accessToken` variable in your environment

### 2. Test Public Endpoints

- **Plans:** Test bundle listing endpoints (no auth required)
- **Blog:** Test blog post endpoints (no auth required)
- **FAQ:** Test FAQ endpoints (no auth required)
- **Health:** Check database health

### 3. Test Protected Endpoints

- **Users:** Get/Update user profile
- **eSIMs:** Activate bundles, get QR codes
- **Orders:** Get order details
- **Admin:** Dashboard stats
- **Blog/FAQ Admin:** CRUD operations

## Collection Structure

```
TTelGo Backend API
├── Authentication (5 endpoints)
│   ├── Request OTP
│   ├── Verify OTP
│   ├── Register
│   ├── Refresh Token
│   └── Logout
├── Users (3 endpoints)
│   ├── Get User by ID
│   ├── Get User by Email
│   └── Update User
├── Plans (6 endpoints)
│   ├── List All Bundles
│   ├── List Bundles by Country
│   ├── Get Bundle Details
│   ├── List Local Bundles
│   ├── List Regional Bundles
│   └── List Global Bundles
├── eSIMs (3 endpoints)
│   ├── Activate Bundle
│   ├── Get QR Code
│   └── Get Order Details
├── Orders (3 endpoints)
│   ├── Get Order by ID
│   ├── Get Order by Reference
│   └── Get Orders by User ID
├── Blog (9 endpoints)
│   ├── Get All Posts (public)
│   ├── Get Post by Slug
│   ├── Get Featured Posts
│   ├── Search Posts
│   ├── Get All Posts (Admin)
│   ├── Get Post by ID (Admin)
│   ├── Create Post
│   ├── Update Post
│   └── Delete Post
├── FAQ (7 endpoints)
│   ├── Get All Active FAQs
│   ├── Get FAQ Categories
│   ├── Get All FAQs (Admin)
│   ├── Get FAQ by ID (Admin)
│   ├── Create FAQ
│   ├── Update FAQ
│   └── Delete FAQ
├── Admin (1 endpoint)
│   └── Get Dashboard Stats
└── Health (2 endpoints)
    ├── Check Database Health
    └── Spring Actuator Health
```

## Authentication Flow

1. **Request OTP:**
   ```json
   POST /api/auth/otp/request
   {
     "email": "user@example.com",
     "phone": "+1234567890",
     "purpose": "LOGIN"
   }
   ```

2. **Verify OTP:**
   ```json
   POST /api/auth/otp/verify
   {
     "email": "user@example.com",
     "phone": "+1234567890",
     "otp": "123456"
   }
   ```
   Response includes `token` - copy this to `accessToken` variable

3. **Use Token:**
   All protected endpoints automatically use `{{accessToken}}` in Authorization header

## Tips

1. **Save Responses:** Right-click on requests → Save Response → Save as Example
2. **Tests:** Add tests to automatically extract and save tokens
3. **Pre-request Scripts:** Automate token refresh
4. **Collection Runner:** Run multiple requests in sequence
5. **Documentation:** Each endpoint has descriptions - read them for details

## Example Test Script (Auto-save Token)

Add this to **"Verify OTP"** request → **Tests** tab:

```javascript
if (pm.response.code === 200) {
    const response = pm.response.json();
    if (response.data && response.data.token) {
        pm.environment.set("accessToken", response.data.token);
        console.log("Token saved to environment");
    }
}
```

## Troubleshooting

- **401 Unauthorized:** Token expired or missing - re-authenticate
- **403 Forbidden:** Insufficient permissions - check user role
- **404 Not Found:** Check baseUrl and endpoint path
- **500 Internal Server Error:** Check server logs and database connection

## API Response Format

All endpoints return responses in this format:

```json
{
  "success": true,
  "message": "Success",
  "data": { ... },
  "errors": null
}
```

Error responses:

```json
{
  "success": false,
  "message": "Error message",
  "data": null,
  "errors": ["Error details"]
}
```

