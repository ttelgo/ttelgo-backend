# User Profile API Implementation

## Overview

This document describes the implementation of user profile APIs for fetching and updating user profile information.

---

## API Endpoints

### 1. Get User Profile

**Endpoint:** `GET /api/user/profile/{userId}`

**Description:** Fetches the profile information for a specific user by their ID.

**Path Parameters:**
- `userId` (Long, required) - The ID of the user whose profile to fetch

**Response:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 1,
    "name": "John Doe",
    "email": "john.doe@example.com",
    "phone": "+1234567890",
    "firstName": "John",
    "lastName": "Doe",
    "country": "USA",
    "city": "New York",
    "address": "123 Main St",
    "postalCode": "10001",
    "isEmailVerified": true,
    "isPhoneVerified": false,
    "referralCode": "ABC12345",
    "referredBy": null,
    "role": "USER",
    "createdAt": "2026-01-27T10:00:00",
    "updatedAt": "2026-01-27T10:00:00"
  },
  "timestamp": "2026-01-27T16:00:00Z"
}
```

**Error Responses:**
- `404 Not Found`: User not found with the specified ID
```json
{
  "success": false,
  "message": "User not found with ID: 999",
  "error": {
    "code": "ERR_1002",
    "message": "Resource Not Found"
  }
}
```

---

### 2. Update User Profile

**Endpoint:** `PUT /api/user/profile`

**Description:** Updates user profile information. Validates that user exists before updating. Automatically updates `updatedAt` timestamp.

**Request Body:**
```json
{
  "userId": 1,
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1234567890",
  "email": "john.doe@example.com"
}
```

**Request Body Fields:**
- `userId` (Long, required) - The ID of the user to update
- `firstName` (String, optional) - User's first name
- `lastName` (String, optional) - User's last name
- `phone` (String, optional) - User's phone number
- `email` (String, optional) - User's email address (must be valid email format)

**Response:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 1,
    "name": "John Doe",
    "email": "john.doe@example.com",
    "phone": "+1234567890",
    "firstName": "John",
    "lastName": "Doe",
    "country": "USA",
    "city": "New York",
    "address": "123 Main St",
    "postalCode": "10001",
    "isEmailVerified": true,
    "isPhoneVerified": false,
    "referralCode": "ABC12345",
    "referredBy": null,
    "role": "USER",
    "createdAt": "2026-01-27T10:00:00",
    "updatedAt": "2026-01-27T16:00:00"
  },
  "timestamp": "2026-01-27T16:00:00Z"
}
```

**Error Responses:**

1. **400 Bad Request** - Validation errors
```json
{
  "success": false,
  "message": "Validation failed",
  "error": {
    "code": "ERR_1001",
    "message": "Invalid Request"
  }
}
```

2. **404 Not Found** - User not found
```json
{
  "success": false,
  "message": "User not found with ID: 999",
  "error": {
    "code": "ERR_1002",
    "message": "Resource Not Found"
  }
}
```

3. **409 Conflict** - Email or phone already in use
```json
{
  "success": false,
  "message": "Email already in use by another user",
  "error": {
    "code": "ERR_1003",
    "message": "User Already Exists"
  }
}
```

---

## Implementation Details

### Files Created/Modified

1. **ProfileController.java** (`src/main/java/com/tiktel/ttelgo/user/api/ProfileController.java`)
   - New controller for profile endpoints
   - Handles GET and PUT requests for user profiles

2. **UpdateProfileRequest.java** (`src/main/java/com/tiktel/ttelgo/user/api/dto/UpdateProfileRequest.java`)
   - DTO for update profile request
   - Contains userId, firstName, lastName, phone, email
   - Includes validation annotations

3. **UserService.java** (Modified)
   - Added `updateUserProfile()` method
   - Handles email and phone uniqueness validation
   - Updates `updatedAt` timestamp automatically via `@PreUpdate`

### Existing Files Used

1. **UserRepository.java** - Already exists, provides JPA repository methods
2. **UserRepositoryPort.java** - Already exists, provides port interface
3. **UserResponse.java** - Already exists, DTO for user response
4. **User.java** - Already exists, User entity with `@PreUpdate` for `updatedAt`

---

## Features

### ✅ Validation
- User existence validation before update
- Email format validation
- Email uniqueness validation (cannot be used by another user)
- Phone uniqueness validation (cannot be used by another user)
- Required field validation (userId)

### ✅ Automatic Timestamp Updates
- `updatedAt` is automatically updated via `@PreUpdate` annotation in User entity
- No manual timestamp handling required

### ✅ Transaction Management
- All database operations are transactional
- Ensures data consistency

### ✅ Error Handling
- Proper exception handling with meaningful error messages
- Returns appropriate HTTP status codes
- Uses existing error code system

---

## Testing Examples

### Using cURL

**Get User Profile:**
```bash
curl -X GET "http://localhost:8080/api/user/profile/1" \
  -H "Content-Type: application/json"
```

**Update User Profile:**
```bash
curl -X PUT "http://localhost:8080/api/user/profile" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "firstName": "John",
    "lastName": "Doe",
    "phone": "+1234567890",
    "email": "john.doe@example.com"
  }'
```

### Using Postman

**Get User Profile:**
1. Method: `GET`
2. URL: `http://localhost:8080/api/user/profile/1`
3. Headers: `Content-Type: application/json`

**Update User Profile:**
1. Method: `PUT`
2. URL: `http://localhost:8080/api/user/profile`
3. Headers: `Content-Type: application/json`
4. Body (raw JSON):
```json
{
  "userId": 1,
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1234567890",
  "email": "john.doe@example.com"
}
```

---

## Database Schema

The implementation uses the existing `users` table with the following relevant fields:

- `id` (BIGSERIAL PRIMARY KEY)
- `email` (VARCHAR(255) UNIQUE NOT NULL)
- `phone` (VARCHAR(50) UNIQUE)
- `first_name` (VARCHAR(100))
- `last_name` (VARCHAR(100))
- `updated_at` (TIMESTAMP) - Automatically updated via `@PreUpdate`

---

## Security Considerations

**Note:** As per requirements, these endpoints do not require JWT authentication. The `userId` is passed directly in the path parameter or request body.

**For Production:**
- Consider adding authentication/authorization
- Validate that users can only update their own profiles
- Add rate limiting to prevent abuse
- Consider adding audit logging

---

## Summary

✅ **GET /api/user/profile/{userId}** - Fetch user profile  
✅ **PUT /api/user/profile** - Update user profile  
✅ User existence validation  
✅ Email and phone uniqueness validation  
✅ Automatic `updatedAt` timestamp updates  
✅ Proper error handling  
✅ Transaction management  

**Status:** ✅ Implementation Complete

---

**Last Updated:** 2026-01-27
