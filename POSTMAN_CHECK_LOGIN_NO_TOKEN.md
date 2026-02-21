# How to Check Login Status in Postman (Without Manual Token Entry)

This guide shows you how to check if a user is logged in using Postman **without manually entering tokens**. Postman will automatically handle tokens for you!

---

## 🎯 Quick Setup (One Time - 2 Minutes)

### Step 1: Create Environment Variable

1. In Postman, click **Environments** (left sidebar) or press `Ctrl+E`
2. Click **+** (Create Environment)
3. Name it: `TTelgo Backend`
4. Add variable:
   - **Variable:** `jwt_token`
   - **Initial Value:** (leave empty)
   - **Current Value:** (leave empty)
5. Click **Save**
6. **IMPORTANT:** Select `TTelgo Backend` from dropdown (top-right corner)

---

### Step 2: Setup Collection-Level Authorization (Automatic Token Usage)

1. **Right-click** on your collection name (e.g., "TTelGo API")
2. Click **Edit**
3. Go to **Authorization** tab
4. Select **Type:** `Bearer Token`
5. In **Token** field, enter: `{{jwt_token}}`
6. Click **Update**

✅ **Done!** Now ALL requests in this collection will automatically use the token!

---

### Step 3: Add Auto-Token Extraction to Login Request

1. Open your **Login/Verify OTP** request:
   ```
   POST http://localhost:8080/api/v1/auth/otp/verify
   ```

2. Go to **Tests** tab (at the bottom of the request)

3. **Paste this script:**

```javascript
// Auto-extract and save access token from login response
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    
    // Extract accessToken from response
    var accessToken = jsonData.data?.accessToken || jsonData.data?.token;
    
    if (accessToken) {
        // Save to environment variable (automatically used by all requests)
        pm.environment.set("jwt_token", accessToken);
        pm.collectionVariables.set("jwt_token", accessToken);
        
        console.log("✅ Token automatically saved!");
        console.log("You can now check login status without entering token!");
    } else {
        console.error("❌ No accessToken found in response");
    }
} else {
    console.error("❌ Login failed: " + pm.response.text());
}
```

4. Click **Save**

---

## 🚀 How to Check Login Status (No Manual Token Needed!)

### Method 1: Use `/api/v1/users/me` Endpoint

1. **Create or open this request:**
   ```
   GET http://localhost:8080/api/v1/users/me
   ```

2. **That's it!** No Authorization header needed - it's automatically added from collection settings!

3. **Click Send**

### ✅ If User is Logged In:
**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "phone": "+1234567890",
    "firstName": "John",
    "lastName": "Doe",
    "role": "USER",
    "userType": "CUSTOMER",
    "isEmailVerified": true,
    "isPhoneVerified": false
  }
}
```

### ❌ If User is NOT Logged In:
**Response (401 Unauthorized):**
```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Authentication required. Please provide a valid authentication token."
  }
}
```

---

## 📋 Complete Workflow

### First Time Setup:
1. ✅ Create environment variable `jwt_token`
2. ✅ Set collection authorization to use `{{jwt_token}}`
3. ✅ Add auto-extract script to login request

### Every Time You Want to Check Login:

**Option A: If you already logged in:**
1. Just run: `GET /api/v1/users/me`
2. Check response:
   - **200 OK** = User is logged in ✅
   - **401 Unauthorized** = User is NOT logged in ❌

**Option B: If you need to login first:**
1. Run: `POST /api/v1/auth/otp/verify` (with email and OTP)
2. Token is automatically saved! ✅
3. Run: `GET /api/v1/users/me`
4. Token is automatically used! ✅

---

## 🎯 Visual Step-by-Step

### 1. Login Request (Auto-Saves Token)
```
POST /api/v1/auth/otp/verify
Body: { "email": "user@example.com", "otp": "123456" }
Tests Tab: [Script auto-saves token]
```

### 2. Check Login Status (Auto-Uses Token)
```
GET /api/v1/users/me
Authorization: [Automatically added from collection]
```

**No manual token entry needed!** 🎉

---

## ✅ Verify Setup is Working

### Check 1: Token is Saved
1. Click **Environments** (left sidebar)
2. Select your environment
3. Look for `jwt_token` variable
4. Should have a long token string (if you've logged in)

### Check 2: Token is Being Used
1. Run `GET /api/v1/users/me`
2. Open **Console** (View → Show Postman Console)
3. Check the request - should show:
   ```
   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```

### Check 3: Response Status
- **200 OK** = Token is valid, user is logged in ✅
- **401 Unauthorized** = Token missing/invalid, user is NOT logged in ❌

---

## 🔧 Troubleshooting

### Problem: Getting 401 Unauthorized

**Solution 1: Check if token is saved**
- Go to Environments → Check `jwt_token` variable
- If empty, run login request first

**Solution 2: Check collection authorization**
- Right-click collection → Edit → Authorization
- Should be: Type: Bearer Token, Token: `{{jwt_token}}`

**Solution 3: Check environment is selected**
- Top-right dropdown should show your environment name
- If not selected, select it

**Solution 4: Token might be expired**
- Tokens expire after 24 hours
- Run login request again to get new token

### Problem: Token not being saved

**Check:**
1. Open **Console** (View → Show Postman Console)
2. Run login request
3. Look for error messages
4. Check if response has `data.accessToken` field

**Fix:**
- Make sure Tests script is saved
- Check response format matches script expectations

---

## 📝 Quick Reference

| Action | What to Do |
|--------|-----------|
| **Check if logged in** | Run `GET /api/v1/users/me` |
| **Login first** | Run `POST /api/v1/auth/otp/verify` |
| **200 OK** | ✅ User is logged in |
| **401 Unauthorized** | ❌ User is NOT logged in |
| **Token auto-saved?** | Check Environments → `jwt_token` |
| **Token auto-used?** | Check Console → Request headers |

---

## 🎉 Summary

**After setup:**
- ✅ Login once → Token automatically saved
- ✅ Check login status → Just call `/api/v1/users/me`
- ✅ **No manual token entry ever needed!**
- ✅ **No Authorization header setup needed!**

**You're done! Just call the API and Postman handles everything!** 🚀

---

## 💡 Pro Tips

1. **Use Collection Runner:** Run login + check status in sequence
2. **Save as Example:** Save successful responses as examples
3. **Use Tests:** Add test scripts to automatically verify login status:
   ```javascript
   pm.test("User is logged in", function () {
       pm.response.to.have.status(200);
       var jsonData = pm.response.json();
       pm.expect(jsonData.data).to.have.property('email');
   });
   ```

---

## 🔗 Related Files

- See `POSTMAN_AUTO_TOKEN_QUICK_SETUP.md` for detailed token setup
- See `HOW_TO_CHECK_USER_LOGIN.md` for general login checking guide
- See `JWT_AUTHENTICATION_TROUBLESHOOTING.md` for troubleshooting
















