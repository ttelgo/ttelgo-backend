# OTP Request API - Stores Data in Database (No Token Required)

Yes! The `/api/v1/auth/otp/request` API **does store data in the database** and **does NOT require a token** (it's a public endpoint).

---

## ✅ Quick Answer

- ✅ **No token required** - It's a public endpoint
- ✅ **Stores data in database** - Saves to `otp_tokens` table
- ✅ **Can be called directly** - No authentication needed

---

## 📊 What Data Gets Stored?

When you call `POST /api/v1/auth/otp/request`, the following data is stored in the `otp_tokens` table:

| Field | Description | Example |
|-------|-------------|---------|
| `id` | Auto-generated primary key | `1` |
| `email` | User's email address | `user@example.com` |
| `phone` | User's phone number (optional) | `+1234567890` |
| `otp_code` | Hashed OTP code (for security) | `$2a$10$hashed...` |
| `purpose` | Purpose of OTP | `LOGIN`, `REGISTER`, `RESET_PASSWORD`, etc. |
| `is_used` | Whether OTP has been used | `false` |
| `attempts` | Number of verification attempts | `0` |
| `max_attempts` | Maximum allowed attempts | `3` |
| `expires_at` | OTP expiration timestamp | `2024-01-16 18:10:00` |
| `created_at` | Record creation timestamp | `2024-01-16 18:00:00` |

---

## 🔍 How It Works

### Step 1: Call the API (No Token Needed)

```bash
POST http://localhost:8080/api/v1/auth/otp/request
Content-Type: application/json

{
  "email": "user@example.com",
  "phone": null,
  "purpose": "LOGIN"
}
```

**Response:**
```json
{
  "success": true,
  "message": "OTP sent successfully",
  "data": "OTP sent successfully"
}
```

### Step 2: What Happens Behind the Scenes

1. **Generates OTP:** Creates a 6-digit OTP code
2. **Hashes OTP:** Encrypts the OTP for security (stored as `otp_code`)
3. **Invalidates Old OTPs:** Deletes any unused OTPs for the same email/phone
4. **Saves to Database:** Stores new OTP record in `otp_tokens` table
5. **Sends OTP:** Sends plain OTP to user via email/SMS

### Step 3: Database Record Created

A new record is inserted into the `otp_tokens` table:

```sql
INSERT INTO otp_tokens (
    email, 
    phone, 
    otp_code, 
    purpose, 
    is_used, 
    attempts, 
    max_attempts, 
    expires_at, 
    created_at
) VALUES (
    'user@example.com',
    NULL,
    '$2a$10$hashed_otp_code...',  -- Hashed OTP
    'LOGIN',
    false,
    0,
    3,
    '2024-01-16 18:10:00',  -- 10 minutes from now
    '2024-01-16 18:00:00'
);
```

---

## 📋 Complete Example

### JavaScript/React Example:

```javascript
// No token needed - just call the API directly
const requestOtp = async (email, purpose = 'LOGIN') => {
  try {
    const response = await fetch('http://localhost:8080/api/v1/auth/otp/request', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        // ✅ No Authorization header needed!
      },
      body: JSON.stringify({
        email: email,
        phone: null,  // Optional
        purpose: purpose  // LOGIN, REGISTER, RESET_PASSWORD, etc.
      })
    });
    
    const result = await response.json();
    
    if (result.success) {
      console.log('✅ OTP sent successfully');
      console.log('📊 Data stored in database: otp_tokens table');
      return true;
    } else {
      console.error('❌ Failed to send OTP:', result.message);
      return false;
    }
  } catch (error) {
    console.error('❌ Error:', error);
    return false;
  }
};

// Usage
await requestOtp('user@example.com', 'LOGIN');
```

### Postman Example:

1. **Create Request:**
   ```
   POST http://localhost:8080/api/v1/auth/otp/request
   ```

2. **Headers:**
   ```
   Content-Type: application/json
   ```
   (No Authorization header needed!)

3. **Body:**
   ```json
   {
     "email": "user@example.com",
     "purpose": "LOGIN"
   }
   ```

4. **Click Send** - Data is stored in database!

### curl Example:

```bash
# No token needed - just call directly
curl -X POST http://localhost:8080/api/v1/auth/otp/request \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "purpose": "LOGIN"
  }'
```

---

## 🗄️ Database Table Structure

The `otp_tokens` table structure:

```sql
CREATE TABLE otp_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255),
    phone VARCHAR(255),
    otp_code VARCHAR(255) NOT NULL,  -- Hashed OTP
    purpose VARCHAR(50),              -- LOGIN, REGISTER, etc.
    is_used BOOLEAN DEFAULT FALSE,
    attempts INT DEFAULT 0,
    max_attempts INT DEFAULT 3,
    expires_at DATETIME NOT NULL,
    created_at DATETIME
);
```

---

## 🔐 Security Features

### 1. OTP is Hashed Before Storage
- Plain OTP is never stored in database
- Only hashed version is saved (using BCrypt)
- Example: OTP `123456` → Stored as `$2a$10$hashed...`

### 2. Automatic Expiration
- OTP expires after **10 minutes** by default
- Expired OTPs cannot be used

### 3. Attempt Limiting
- Maximum **3 attempts** to verify OTP
- After 3 failed attempts, OTP is invalidated

### 4. Old OTP Invalidation
- When requesting new OTP, old unused OTPs are deleted
- Prevents multiple active OTPs for same email/phone

---

## 📝 Request Body Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `email` | String | Optional* | User's email address |
| `phone` | String | Optional* | User's phone number (E.164 format) |
| `purpose` | String | Optional | Purpose: `LOGIN`, `REGISTER`, `RESET_PASSWORD`, `VERIFY_EMAIL`, `VERIFY_PHONE` (default: `LOGIN`) |

**Note:** At least one of `email` or `phone` must be provided.

---

## ✅ Verification: Check Database

After calling the API, you can verify data was stored:

### Using SQL:

```sql
-- Check latest OTP token
SELECT * FROM otp_tokens 
ORDER BY created_at DESC 
LIMIT 1;

-- Check all OTPs for an email
SELECT * FROM otp_tokens 
WHERE email = 'user@example.com' 
ORDER BY created_at DESC;
```

### Expected Result:

```
id | email              | phone | otp_code              | purpose | is_used | attempts | expires_at          | created_at
---|--------------------|-------|-----------------------|---------|---------|----------|---------------------|-------------------
1  | user@example.com   | NULL  | $2a$10$hashed...      | LOGIN   | false   | 0        | 2024-01-16 18:10:00 | 2024-01-16 18:00:00
```

---

## 🔄 Complete Flow

```
1. User calls: POST /api/v1/auth/otp/request
   ↓
2. Server generates 6-digit OTP
   ↓
3. Server hashes OTP for security
   ↓
4. Server deletes old unused OTPs for same email/phone
   ↓
5. Server saves new OTP record to database (otp_tokens table)
   ↓
6. Server sends plain OTP to user via email/SMS
   ↓
7. User receives OTP code
   ↓
8. User calls: POST /api/v1/auth/otp/verify (with OTP code)
   ↓
9. Server verifies OTP against database record
   ↓
10. Server marks OTP as used and returns JWT token
```

---

## ⚠️ Important Notes

### 1. OTP Expires Quickly
- OTP expires in **10 minutes**
- Request new OTP if expired

### 2. Limited Attempts
- Maximum **3 verification attempts**
- After 3 failed attempts, request new OTP

### 3. One Active OTP Per Email/Phone
- Requesting new OTP invalidates previous unused OTPs
- Only one active OTP at a time

### 4. OTP is Hashed
- You cannot read the actual OTP from database
- OTP is sent to user via email/SMS only

---

## 🎯 Use Cases

### 1. User Registration
```javascript
await requestOtp('newuser@example.com', 'REGISTER');
```

### 2. User Login
```javascript
await requestOtp('user@example.com', 'LOGIN');
```

### 3. Password Reset
```javascript
await requestOtp('user@example.com', 'RESET_PASSWORD');
```

### 4. Email Verification
```javascript
await requestOtp('user@example.com', 'VERIFY_EMAIL');
```

### 5. Phone Verification
```javascript
await requestOtp(null, 'VERIFY_PHONE', '+1234567890');
```

---

## 📊 Summary

| Question | Answer |
|----------|--------|
| **Requires Token?** | ❌ No - Public endpoint |
| **Stores Data?** | ✅ Yes - Saves to `otp_tokens` table |
| **What Data?** | Email, phone, hashed OTP, purpose, expiration, attempts |
| **Can Call Directly?** | ✅ Yes - No authentication needed |
| **Database Table?** | `otp_tokens` |
| **OTP Expires?** | ✅ Yes - 10 minutes |
| **Max Attempts?** | 3 attempts |

---

## 🔗 Related Endpoints

- **Request OTP:** `POST /api/v1/auth/otp/request` (No token needed)
- **Verify OTP:** `POST /api/v1/auth/otp/verify` (No token needed, returns JWT token)
- **Get User Info:** `GET /api/v1/users/me` (Requires JWT token)

---

**You can call `/api/v1/auth/otp/request` directly without any token, and it will store the OTP data in the database!** 🚀
















