# User Profile API - Testing Guide

## Issue: "User not found with ID: 1"

This error means the endpoint is working correctly, but there's no user with ID `1` in your database.

---

## Solution 1: Find Existing Users

### Option A: Find User by Email

Use the existing endpoint to find a user by email:

```http
GET /api/v1/users?email=your-email@example.com
Content-Type: application/json
```

**Example:**
```http
GET http://localhost:8080/api/v1/users?email=admin@ttelgo.com
```

This will return the user's ID, which you can then use in the profile endpoint.

### Option B: Check Database Directly

Connect to your PostgreSQL database and run:

```sql
SELECT id, email, name, first_name, last_name 
FROM users 
ORDER BY id 
LIMIT 10;
```

This will show you all users and their IDs.

---

## Solution 2: Create a Test User

### Method 1: Using OTP Authentication (Recommended)

1. **Request OTP:**
```http
POST /api/v1/auth/otp/request
Content-Type: application/json

{
  "email": "test@example.com",
  "purpose": "LOGIN"
}
```

2. **Verify OTP:**
```http
POST /api/v1/auth/otp/verify
Content-Type: application/json

{
  "email": "test@example.com",
  "otp": "123456"
}
```

This will create a user and return the user ID in the response.

### Method 2: Create Admin User via API

```http
POST /api/v1/auth/admin/create-initial
Content-Type: application/json
```

This creates an admin user with:
- Email: `admin@ttelgo.com`
- Password: `Admin@123456`
- ID: Will be auto-generated (check response)

### Method 3: Create User Directly in Database

```sql
INSERT INTO users (
    email,
    name,
    first_name,
    last_name,
    phone,
    role,
    is_email_verified,
    created_at,
    updated_at
) VALUES (
    'test@example.com',
    'Test User',
    'Test',
    'User',
    '+1234567890',
    'USER',
    true,
    NOW(),
    NOW()
) RETURNING id;
```

The `RETURNING id;` will show you the created user's ID.

---

## Solution 3: Use the Correct User ID

After finding or creating a user, use their actual ID:

**Example:**
If a user has ID `5`, use:
```http
GET /api/user/profile/5
```

---

## Quick Test Steps

1. **Check if admin user exists:**
```http
GET /api/v1/users?email=admin@ttelgo.com
```

2. **If no users exist, create one:**
```http
POST /api/v1/auth/admin/create-initial
```

3. **Get the user ID from the response and use it:**
```http
GET /api/user/profile/{actualUserId}
```

---

## Example: Complete Flow

### Step 1: Create a User
```http
POST http://localhost:8080/api/v1/auth/otp/request
Content-Type: application/json

{
  "email": "testuser@example.com",
  "purpose": "LOGIN"
}
```

### Step 2: Verify OTP (Get User ID)
```http
POST http://localhost:8080/api/v1/auth/otp/verify
Content-Type: application/json

{
  "email": "testuser@example.com",
  "otp": "123456"
}
```

**Response will include:**
```json
{
  "success": true,
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "user": {
      "id": 5,  // <-- Use this ID
      "email": "testuser@example.com",
      ...
    }
  }
}
```

### Step 3: Use the User ID
```http
GET http://localhost:8080/api/user/profile/5
Content-Type: application/json
```

---

## Database Query to List All Users

```sql
-- List all users with their IDs
SELECT 
    id,
    email,
    name,
    first_name,
    last_name,
    phone,
    role,
    created_at
FROM users
ORDER BY id;
```

---

## Common Issues

### Issue: "User not found with ID: X"

**Solution:**
- Check if the user exists using: `GET /api/v1/users?email=user@example.com`
- Or query the database: `SELECT id, email FROM users WHERE id = X;`
- Use a valid user ID that exists in your database

### Issue: No users in database

**Solution:**
- Create a user using OTP authentication (Method 1 above)
- Or create admin user using: `POST /api/v1/auth/admin/create-initial`
- Or insert directly in database (Method 3 above)

---

## Testing Checklist

- [ ] Check if users exist: `GET /api/v1/users?email=known-email@example.com`
- [ ] If no users, create one via OTP or admin endpoint
- [ ] Get the user ID from the response
- [ ] Test profile endpoint with actual user ID: `GET /api/user/profile/{userId}`
- [ ] Test update endpoint: `PUT /api/user/profile` with correct userId

---

**Note:** User IDs are auto-generated and may not start at 1. Always use the actual user ID from your database.

