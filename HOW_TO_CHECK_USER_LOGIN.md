# How to Check if User is Logged In

This guide explains different ways to check if a user is logged in, both on the backend (in your code) and from the frontend/API.

---

## 🔍 Method 1: Using the `/api/v1/users/me` Endpoint (Recommended for Frontend/API)

The easiest way to check if a user is logged in is to call the `/api/v1/users/me` endpoint.

### ✅ If User is Logged In:
```bash
GET http://localhost:8080/api/v1/users/me
Authorization: Bearer <your_access_token>
```

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

### Example: Frontend JavaScript
```javascript
async function checkIfUserIsLoggedIn() {
  try {
    const token = localStorage.getItem('accessToken');
    
    if (!token) {
      console.log('User is NOT logged in - No token found');
      return false;
    }
    
    const response = await fetch('http://localhost:8080/api/v1/users/me', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (response.ok) {
      const data = await response.json();
      console.log('User IS logged in:', data.data);
      return true;
    } else {
      console.log('User is NOT logged in - Invalid token');
      return false;
    }
  } catch (error) {
    console.error('Error checking login status:', error);
    return false;
  }
}

// Usage
checkIfUserIsLoggedIn().then(isLoggedIn => {
  if (isLoggedIn) {
    console.log('User is authenticated!');
  } else {
    console.log('User needs to login');
  }
});
```

### Example: Using curl
```bash
# Check if logged in
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json"

# If 200 OK → User is logged in
# If 401 Unauthorized → User is NOT logged in
```

---

## 🔍 Method 2: Backend - Check Authentication in Controllers/Services

If you're writing backend code (Java), here are several ways to check if a user is logged in:

### Option A: Using SecurityContextHolder (Direct Check)

```java
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public boolean isUserLoggedIn() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    
    if (authentication == null || !authentication.isAuthenticated()) {
        return false;
    }
    
    // Check if it's not an anonymous user
    String principalName = authentication.getName();
    if (principalName == null || principalName.equals("anonymousUser")) {
        return false;
    }
    
    return true;
}
```

### Option B: Using RoleScopeResolver (Recommended)

The `RoleScopeResolver` utility class provides convenient methods:

```java
import com.tiktel.ttelgo.security.RoleScopeResolver;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
public class MyController {
    
    @Autowired
    private RoleScopeResolver roleScopeResolver;
    
    @GetMapping("/check-login")
    public ResponseEntity<?> checkLoginStatus() {
        // Get current user ID - returns null if not logged in
        Long userId = roleScopeResolver.getCurrentUserId();
        
        if (userId == null) {
            return ResponseEntity.status(401).body("User is NOT logged in");
        }
        
        return ResponseEntity.ok("User is logged in with ID: " + userId);
    }
}
```

### Option C: Get Current User Information

```java
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.tiktel.ttelgo.security.JwtAuthenticationDetails;
import com.tiktel.ttelgo.security.UserPrincipal;

@GetMapping("/my-info")
public ResponseEntity<?> getMyInfo() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    
    if (authentication == null || !authentication.isAuthenticated()) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "User is not logged in");
    }
    
    // Get user ID from principal
    if (authentication.getPrincipal() instanceof UserPrincipal) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getId();
        String email = userPrincipal.getUsername();
        
        // Get additional info from authentication details
        if (authentication.getDetails() instanceof JwtAuthenticationDetails) {
            JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();
            String userType = details.getUserType();
            String firstName = details.getFirstName();
            String lastName = details.getLastName();
            
            // Use the information...
        }
    }
    
    return ResponseEntity.ok("User is logged in");
}
```

---

## 🔍 Method 3: Check Authentication Status Endpoint

You can create a simple endpoint to check authentication status:

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthStatusController {
    
    @Autowired
    private RoleScopeResolver roleScopeResolver;
    
    /**
     * Check if current user is authenticated.
     * GET /api/v1/auth/status
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAuthStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        Map<String, Object> status = new HashMap<>();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            status.put("isAuthenticated", false);
            status.put("message", "User is not logged in");
            return ResponseEntity.ok(ApiResponse.success(status));
        }
        
        // User is authenticated
        status.put("isAuthenticated", true);
        
        Long userId = roleScopeResolver.getCurrentUserId();
        status.put("userId", userId);
        
        if (authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            status.put("email", userPrincipal.getUsername());
        }
        
        if (authentication.getDetails() instanceof JwtAuthenticationDetails) {
            JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();
            status.put("userType", details.getUserType());
            status.put("firstName", details.getFirstName());
            status.put("lastName", details.getLastName());
        }
        
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
```

**Usage:**
```bash
GET http://localhost:8080/api/v1/auth/status
Authorization: Bearer <token>
```

**Response if logged in:**
```json
{
  "success": true,
  "data": {
    "isAuthenticated": true,
    "userId": 1,
    "email": "user@example.com",
    "userType": "CUSTOMER",
    "firstName": "John",
    "lastName": "Doe"
  }
}
```

**Response if NOT logged in:**
```json
{
  "success": true,
  "data": {
    "isAuthenticated": false,
    "message": "User is not logged in"
  }
}
```

---

## 🔍 Method 4: Using Spring Security Annotations

Spring Security provides annotations to protect endpoints:

```java
@RestController
@RequestMapping("/api/v1/protected")
public class ProtectedController {
    
    /**
     * This endpoint requires authentication.
     * If user is not logged in, Spring Security will return 401 automatically.
     */
    @GetMapping("/data")
    public ResponseEntity<?> getProtectedData() {
        // If code reaches here, user IS logged in
        // Spring Security already validated the token
        
        Long userId = roleScopeResolver.getCurrentUserId();
        return ResponseEntity.ok("Protected data for user: " + userId);
    }
}
```

---

## 📋 Quick Reference

### Frontend/API Check:
- ✅ **Use:** `GET /api/v1/users/me` with Authorization header
- ✅ **200 OK** = User is logged in
- ❌ **401 Unauthorized** = User is NOT logged in

### Backend Check:
```java
// Simple check
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
boolean isLoggedIn = auth != null && auth.isAuthenticated();

// Get user ID (returns null if not logged in)
Long userId = roleScopeResolver.getCurrentUserId();

// Check if user has specific role
boolean isAdmin = roleScopeResolver.hasRole("ADMIN");
```

---

## 🎯 Common Use Cases

### 1. Check Before Showing Protected Content (Frontend)
```javascript
async function loadProtectedContent() {
  const isLoggedIn = await checkIfUserIsLoggedIn();
  
  if (isLoggedIn) {
    // Show protected content
    loadUserDashboard();
  } else {
    // Redirect to login
    window.location.href = '/login';
  }
}
```

### 2. Conditional Logic in Backend
```java
@GetMapping("/dashboard")
public ResponseEntity<?> getDashboard() {
    Long userId = roleScopeResolver.getCurrentUserId();
    
    if (userId == null) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "Please login first");
    }
    
    // User is logged in, proceed with dashboard data
    return ResponseEntity.ok(dashboardService.getDashboard(userId));
}
```

### 3. Check User Role
```java
@GetMapping("/admin-panel")
public ResponseEntity<?> getAdminPanel() {
    if (!roleScopeResolver.hasRole("ADMIN")) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "Admin access required");
    }
    
    // User is admin, show admin panel
    return ResponseEntity.ok(adminService.getAdminData());
}
```

---

## ⚠️ Important Notes

1. **Token Expiration:** JWT tokens expire after 24 hours. If a token is expired, the user will appear as "not logged in" even if they have a token.

2. **Token Format:** Always include the `Bearer ` prefix:
   ```
   Authorization: Bearer <token>
   ```

3. **Refresh Token:** Use `accessToken` for API requests, not `refreshToken`.

4. **Automatic Validation:** The `JwtAuthenticationFilter` automatically validates tokens on protected endpoints. You don't need to manually validate tokens in most cases.

---

## 🔗 Related Files

- **UserController:** `src/main/java/com/tiktel/ttelgo/user/api/UserController.java`
- **RoleScopeResolver:** `src/main/java/com/tiktel/ttelgo/security/RoleScopeResolver.java`
- **JwtAuthenticationFilter:** `src/main/java/com/tiktel/ttelgo/security/JwtAuthenticationFilter.java`

---

## 📚 Additional Resources

- See `JWT_AUTHENTICATION_TROUBLESHOOTING.md` for troubleshooting authentication issues
- See `SECURITY_CONFIGURATION.md` for security configuration details
















