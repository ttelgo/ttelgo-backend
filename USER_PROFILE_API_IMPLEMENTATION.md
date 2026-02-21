# Secure User Profile API Implementation

This document describes the secure User Profile API implementation that returns the currently logged-in user's information using JWT authentication.

---

## ✅ Implementation Complete

All required components have been implemented:

1. ✅ **JWT Authentication Filter** - Already extracts `user_id` from JWT and sets it in Spring Security Authentication
2. ✅ **GET /api/v1/users/me** - Secure endpoint that fetches user from database
3. ✅ **User ID Extraction** - Gets userId from Spring Security Authentication (UserPrincipal)
4. ✅ **Database Fetch** - Fetches user data from database using userId
5. ✅ **401 Handling** - Returns 401 if JWT is missing or invalid
6. ✅ **Security** - Users can only access their own profile

---

## 🔐 Security Features

### Authentication Flow

```
1. Frontend sends: Authorization: Bearer <JWT_TOKEN>
   ↓
2. JwtAuthenticationFilter extracts JWT from header
   ↓
3. Filter validates JWT token
   ↓
4. Filter extracts user_id from JWT payload
   ↓
5. Filter loads UserDetails and sets Authentication in SecurityContext
   ↓
6. /api/v1/users/me endpoint reads userId from Authentication
   ↓
7. Endpoint fetches user from database using userId
   ↓
8. Returns user data (only for authenticated user)
```

### Security Guarantees

- ✅ **JWT Required:** Endpoint requires valid JWT token in Authorization header
- ✅ **User Isolation:** Users can only access their own profile (userId from JWT)
- ✅ **No Parameter Injection:** Endpoint does NOT accept userId or email as parameters
- ✅ **401 on Missing Token:** Returns 401 if JWT is missing or invalid
- ✅ **Database Verification:** Always fetches fresh data from database

---

## 📋 API Endpoint

### GET /api/v1/users/me

**Description:** Returns the currently logged-in user's information.

**Authentication:** Required (JWT Bearer token)

**Request Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Parameters:** None (userId is extracted from JWT token)

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "John Doe",
    "email": "user@example.com",
    "phone": "0700000000",
    "firstName": "John",
    "lastName": "Doe",
    "country": "US",
    "city": "New York",
    "address": "123 Main St",
    "postalCode": "10001",
    "isEmailVerified": true,
    "isPhoneVerified": false,
    "referralCode": "ABC123",
    "referredBy": null,
    "role": "USER",
    "userType": "CUSTOMER",
    "createdAt": "2024-01-16T10:00:00",
    "updatedAt": "2024-01-16T10:00:00"
  }
}
```

**Response (401 Unauthorized - No Token):**
```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Authentication required. Please provide a valid authentication token."
  }
}
```

**Response (401 Unauthorized - Invalid Token):**
```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Invalid authentication. User ID not found in authentication context."
  }
}
```

---

## 🔧 Implementation Details

### 1. JWT Authentication Filter

**File:** `src/main/java/com/tiktel/ttelgo/security/JwtAuthenticationFilter.java`

The filter already:
- Extracts JWT from `Authorization: Bearer <token>` header
- Validates the token signature and expiry
- Extracts `user_id` from JWT payload
- Loads UserDetails using `userDetailsService.loadUserById(userId)`
- Sets Authentication in SecurityContext with UserPrincipal

**Key Code:**
```java
Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
UserDetails userDetails = userDetailsService.loadUserById(userId);
SecurityContextHolder.getContext().setAuthentication(authentication);
```

### 2. User Profile Endpoint

**File:** `src/main/java/com/tiktel/ttelgo/user/api/UserController.java`

**Implementation:**
```java
@GetMapping("/me")
public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
    // Get authentication from SecurityContext
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    
    // Check if authenticated
    if (authentication == null || !authentication.isAuthenticated()) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED, 
            "Authentication required. Please provide a valid authentication token.");
    }
    
    // Extract user ID from Authentication (set by JWT filter)
    Long userId = roleScopeResolver.getCurrentUserId();
    
    // Validate user ID
    if (userId == null) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED, 
            "Invalid authentication. User ID not found in authentication context.");
    }
    
    // Fetch user from database (ensures fresh data)
    UserResponse userResponse = userService.getUserById(userId);
    
    return ResponseEntity.ok(ApiResponse.success(userResponse));
}
```

### 3. UserResponse DTO

**File:** `src/main/java/com/tiktel/ttelgo/user/api/dto/UserResponse.java`

Added `name` field to match the requested response format:
```java
private Long id;
private String name;      // ✅ Added
private String email;
private String phone;
// ... other fields
```

### 4. UserService

**File:** `src/main/java/com/tiktel/ttelgo/user/application/UserService.java`

Updated to include `name` in UserResponse mapping:
```java
private UserResponse toUserResponse(User user) {
    return UserResponse.builder()
            .id(user.getId())
            .name(user.getName())  // ✅ Added
            .email(user.getEmail())
            .phone(user.getPhone())
            // ... other fields
            .build();
}
```

---

## 🧪 Testing

### Test with curl

```bash
# 1. First, get JWT token by verifying OTP
curl -X POST http://localhost:8080/api/v1/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "otp": "123456"
  }'

# Response contains accessToken:
# {
#   "success": true,
#   "data": {
#     "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#     ...
#   }
# }

# 2. Use the accessToken to get user profile
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE" \
  -H "Content-Type: application/json"
```

### Test with Postman

1. **Get Token:**
   - POST `/api/v1/auth/otp/verify`
   - Body: `{ "email": "user@example.com", "otp": "123456" }`
   - Copy `accessToken` from response

2. **Get User Profile:**
   - GET `/api/v1/users/me`
   - Authorization: Bearer Token
   - Token: `{{accessToken}}` (or paste token)
   - Send request

### Expected Results

**✅ With Valid Token:**
- Status: 200 OK
- Response: User data from database

**❌ Without Token:**
- Status: 401 Unauthorized
- Message: "Authentication required. Please provide a valid authentication token."

**❌ With Invalid/Expired Token:**
- Status: 401 Unauthorized
- Message: "Invalid authentication. User ID not found in authentication context."

---

## 🔒 Security Validation

### ✅ Requirements Met

1. **JWT Authentication:** ✅ Filter extracts user_id from JWT and sets in Authentication
2. **Database Fetch:** ✅ Endpoint fetches user from database (not from JWT token)
3. **No Parameters:** ✅ Endpoint does NOT accept userId or email as parameters
4. **401 Handling:** ✅ Returns 401 if JWT is missing or invalid
5. **User Isolation:** ✅ Users can only access their own profile (userId from JWT)

### Security Checks

- ✅ **Authentication Required:** Endpoint checks `authentication.isAuthenticated()`
- ✅ **User ID Validation:** Validates userId is not null
- ✅ **Database Verification:** Always fetches from database (fresh data)
- ✅ **No Parameter Injection:** No @PathVariable or @RequestParam for userId/email
- ✅ **JWT Validation:** Handled by JwtAuthenticationFilter before endpoint is called

---

## 📝 Code Changes Summary

### Modified Files

1. **UserResponse.java**
   - Added `name` field

2. **UserService.java**
   - Updated `toUserResponse()` to include `name` field

3. **UserController.java**
   - Updated `/api/v1/users/me` endpoint to:
     - Get userId from Spring Security Authentication
     - Fetch user from database using `userService.getUserById(userId)`
     - Return user data
     - Handle 401 errors properly

### No Changes Needed

- ✅ **JwtAuthenticationFilter** - Already correctly extracts user_id and sets Authentication
- ✅ **SecurityConfig** - Already configured to require authentication for `/api/v1/**`
- ✅ **UserPrincipal** - Already contains user ID

---

## 🎯 API Usage Examples

### Frontend (JavaScript/React)

```javascript
// Get current user profile
const getCurrentUser = async () => {
  const token = localStorage.getItem('accessToken');
  
  if (!token) {
    console.error('No token found. Please login first.');
    return null;
  }
  
  try {
    const response = await fetch('http://localhost:8080/api/v1/users/me', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (response.ok) {
      const result = await response.json();
      console.log('User profile:', result.data);
      return result.data;
    } else if (response.status === 401) {
      console.error('Unauthorized. Token may be expired.');
      localStorage.removeItem('accessToken');
      return null;
    } else {
      console.error('Failed to get user profile:', response.status);
      return null;
    }
  } catch (error) {
    console.error('Error:', error);
    return null;
  }
};
```

### Backend (Java)

```java
// The endpoint automatically:
// 1. Extracts userId from JWT token (via Authentication)
// 2. Fetches user from database
// 3. Returns user data
// No manual userId parameter needed!
```

---

## ✅ Summary

The secure User Profile API is now fully implemented:

- ✅ **Endpoint:** `GET /api/v1/users/me`
- ✅ **Authentication:** JWT Bearer token required
- ✅ **User ID Source:** Extracted from JWT token (via Spring Security Authentication)
- ✅ **Data Source:** Fetched from database (fresh data)
- ✅ **Security:** Users can only access their own profile
- ✅ **Error Handling:** Returns 401 if JWT is missing or invalid
- ✅ **Response Format:** Includes id, name, email, phone, and other user fields

**The API is production-ready and secure!** 🚀
















