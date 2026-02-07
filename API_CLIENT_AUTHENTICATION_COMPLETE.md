# API Client Authentication - Complete Implementation ✅

## Status: **FULLY IMPLEMENTED AND PRODUCTION-READY**

API Client authentication using API Key for vendor or partner access is fully implemented with all security requirements met.

---

## ✅ Requirements Checklist

### API Key Security
- [x] API keys generated securely (SecureRandom + Base64)
- [x] API keys stored as hash (BCrypt)
- [x] Scopes enforced per endpoint ✅
- [x] Rate limiting implemented ✅
- [x] Usage logging per client ✅
- [x] Inactive keys rejected ✅

### Components
- [x] API key filter (`ApiKeyAuthenticationFilter`) ✅
- [x] Client resolver (`ApiClientResolver`) ✅
- [x] Scope validator (`ApiScopeValidator`) ✅
- [x] Example secured endpoint (`VendorOrderController`) ✅

### Authentication Header
- [x] `Authorization: Api-Key {key}` format supported ✅
- [x] Legacy `X-API-Key: {key}` format supported ✅

---

## 📁 File Structure

```
src/main/java/com/tiktel/ttelgo/
├── security/
│   ├── ApiKeyAuthenticationFilter.java    ✅ API key filter
│   ├── ApiKeyAuthenticationDetails.java    ✅ Authentication details
│   ├── ApiClientResolver.java              ✅ Client resolver (NEW)
│   └── ApiScopeValidator.java              ✅ Scope validator (NEW)
├── apikey/
│   ├── domain/
│   │   └── ApiKey.java                     ✅ API key entity
│   ├── application/
│   │   ├── ApiKeyService.java              ✅ Key generation & validation
│   │   └── RateLimitingService.java        ✅ Rate limiting
│   ├── infrastructure/
│   │   └── repository/
│   │       └── ApiKeyRepository.java        ✅ Repository
│   └── api/
│       └── ApiKeyController.java            ✅ Key management
└── order/
    └── api/
        └── VendorOrderController.java       ✅ Example secured endpoint
```

---

## 🔐 Security Implementation

### 1. Secure Key Generation

**Algorithm:**
```java
// Generate 32 random bytes (256 bits)
byte[] bytes = new byte[32];
secureRandom.nextBytes(bytes);
String key = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
return "ttelgo_" + key; // Format: ttelgo_{44-character-base64-string}
```

**Features:**
- ✅ SecureRandom for cryptographically secure randomness
- ✅ Base64 URL encoding (URL-safe)
- ✅ 256-bit entropy (32 bytes)
- ✅ Prefix "ttelgo_" for identification

### 2. Hashed Storage

**Implementation:**
```java
// Store API key as hash (BCrypt)
String keyHash = passwordEncoder.encode(generatedKey);
apiKey.setApiKey(keyHash); // Store hash, not plain key
```

**Validation:**
```java
// Compare incoming key with stored hash
if (passwordEncoder.matches(apiKey, key.getApiKey())) {
    // Key matches
}
```

**Security Benefits:**
- ✅ Keys never stored in plain text
- ✅ Plain key shown only once during creation/regeneration
- ✅ BCrypt hashing (one-way, secure)
- ✅ Timing attack protection

### 3. Scope Enforcement

**Scope Format:**
- `METHOD:/api/v1/endpoint` - Specific method and endpoint
- `/api/v1/endpoint/**` - Path pattern (all methods)
- `*` - Wildcard (all endpoints)

**Examples:**
```json
["GET:/api/v1/vendor/orders", "POST:/api/v1/vendor/orders"]
["/api/v1/vendor/**"]
["*"]
```

**Validation:**
```java
// In ApiKeyAuthenticationFilter
if (!isScopeAllowed(key.getScopes(), requestPath, request.getMethod())) {
    sendErrorResponse(response, 403, "API key does not have permission");
    return;
}
```

### 4. Rate Limiting

**Configuration:**
- Per minute: Default 60 requests
- Per hour: Default 1,000 requests
- Per day: Default 10,000 requests

**Implementation:**
```java
// Check rate limits
if (rateLimitingService.isRateLimitExceeded(key)) {
    sendErrorResponse(response, 429, "Rate limit exceeded");
    return;
}

// Increment counters
rateLimitingService.incrementRequestCount(key.getId(), "minute", now);
```

### 5. Usage Logging

**Logged Information:**
- API key ID
- Endpoint path
- HTTP method
- Status code
- Response time
- IP address
- User agent
- Timestamp
- Error message (if any)

**Implementation:**
```java
ApiUsageLog log = ApiUsageLog.builder()
    .apiKeyId(apiKeyId)
    .endpoint(request.getRequestURI())
    .method(request.getMethod())
    .statusCode(statusCode)
    .ipAddress(getClientIpAddress(request))
    .userAgent(request.getHeader("User-Agent"))
    .createdAt(LocalDateTime.now())
    .build();
usageLogRepository.save(log);
```

### 6. Inactive Key Rejection

**Validation:**
```java
// In ApiKeyService.validateApiKey()
List<ApiKey> activeKeys = apiKeyRepository.findActiveValidKeys();

for (ApiKey key : activeKeys) {
    if (passwordEncoder.matches(apiKey, key.getApiKey())) {
        if (key.isValid()) { // Checks isActive and expiresAt
            return key;
        }
        throw new RuntimeException("API key is expired or inactive");
    }
}
```

**Rejection Conditions:**
- ✅ `isActive = false`
- ✅ `expiresAt < now()`
- ✅ Key not found

---

## 🔧 Components

### 1. ApiKeyAuthenticationFilter

**Location:** `src/main/java/com/tiktel/ttelgo/security/ApiKeyAuthenticationFilter.java`

**Responsibilities:**
- Extract API key from request headers
- Validate API key (hash comparison)
- Check IP whitelist
- Validate scopes
- Check rate limits
- Set authentication in SecurityContext
- Log usage

**Filter Order:** Runs before JWT filter (API key takes precedence)

### 2. ApiClientResolver

**Location:** `src/main/java/com/tiktel/ttelgo/security/ApiClientResolver.java`

**Methods:**
- `getCurrentApiClientId()` - Get API key ID from SecurityContext
- `getCurrentApiClientScopes()` - Get scopes from SecurityContext
- `isApiKeyAuthenticated()` - Check if authenticated via API key
- `getApiKeyPrincipal()` - Get API key principal

**Usage:**
```java
@Autowired
private ApiClientResolver apiClientResolver;

Long apiKeyId = apiClientResolver.getCurrentApiClientId();
List<String> scopes = apiClientResolver.getCurrentApiClientScopes();
```

### 3. ApiScopeValidator

**Location:** `src/main/java/com/tiktel/ttelgo/security/ApiScopeValidator.java`

**Methods:**
- `validateScope(List<String> scopes, String endpoint, String method)` - Validate scope
- `validateCurrentScope(String endpoint, String method, ApiClientResolver resolver)` - Validate from SecurityContext
- `hasActionScope(List<String> scopes, String action)` - Check action scope

**Usage:**
```java
@Autowired
private ApiScopeValidator scopeValidator;

if (!scopeValidator.validateCurrentScope(endpoint, method, apiClientResolver)) {
    throw new AccessDeniedException("Scope not allowed");
}
```

---

## 📝 Example: Secured Endpoint

### VendorOrderController

**Location:** `src/main/java/com/tiktel/ttelgo/order/api/VendorOrderController.java`

**Features:**
- ✅ API key authentication (automatic via filter)
- ✅ Scope validation
- ✅ Client resolution
- ✅ Error handling

**Example:**
```java
@RestController
@RequestMapping("/api/v1/vendor/orders")
@SecurityRequirement(name = "API Key Authentication")
public class VendorOrderController {
    
    @Autowired
    private ApiClientResolver apiClientResolver;
    
    @Autowired
    private ApiScopeValidator scopeValidator;
    
    @PostMapping
    public ApiResponse<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        
        // Get API client information
        Long apiKeyId = apiClientResolver.getCurrentApiClientId();
        if (apiKeyId == null) {
            throw new AccessDeniedException("API key authentication required");
        }
        
        // Validate scope
        String endpoint = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        if (!scopeValidator.validateCurrentScope(endpoint, method, apiClientResolver)) {
            throw new AccessDeniedException("API key does not have permission");
        }
        
        // Business logic...
        return ApiResponse.success(order);
    }
}
```

---

## 🔑 Authentication Header

### Preferred Format

```http
Authorization: Api-Key ttelgo_abc123def456...
```

### Legacy Format (Backward Compatible)

```http
X-API-Key: ttelgo_abc123def456...
```

### Optional: API Secret

```http
Authorization: Api-Key ttelgo_abc123def456...
X-API-Secret: secret123...
```

---

## 📊 Request Flow

```
1. Client sends request with API key
   Authorization: Api-Key ttelgo_abc123...

2. ApiKeyAuthenticationFilter intercepts request
   → Extracts API key from header
   → Validates key (BCrypt comparison)
   → Checks isActive and expiresAt
   → Validates IP whitelist (if configured)
   → Validates scopes (if configured)
   → Checks rate limits
   → Sets authentication in SecurityContext
   → Logs usage

3. Controller receives request
   → Uses ApiClientResolver to get API key ID
   → Uses ApiScopeValidator to validate scopes
   → Processes business logic
   → Returns response

4. Response sent to client
   → Usage logged (status code, response time)
```

---

## 🧪 Testing

### Create API Key

```bash
POST http://localhost:8080/api/v1/admin/api-keys
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "keyName": "Vendor Production Key",
  "customerName": "ABC Vendor",
  "customerEmail": "vendor@example.com",
  "scopes": ["GET:/api/v1/vendor/orders", "POST:/api/v1/vendor/orders"],
  "rateLimitPerMinute": 100,
  "rateLimitPerHour": 5000,
  "rateLimitPerDay": 50000
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "apiKey": "ttelgo_abc123def456...",  // ⚠️ Save this immediately!
    "keyName": "Vendor Production Key",
    "scopes": ["GET:/api/v1/vendor/orders", "POST:/api/v1/vendor/orders"]
  }
}
```

### Use API Key

```bash
# Create order
curl -X POST http://localhost:8080/api/v1/vendor/orders \
  -H "Authorization: Api-Key ttelgo_abc123def456..." \
  -H "Content-Type: application/json" \
  -d '{
    "bundleCode": "BUNDLE_US_1GB",
    "quantity": 10
  }'

# Get orders
curl -X GET http://localhost:8080/api/v1/vendor/orders \
  -H "Authorization: Api-Key ttelgo_abc123def456..."
```

---

## 🛡️ Security Features Summary

| Feature | Status | Implementation |
|---------|--------|----------------|
| Secure key generation | ✅ | SecureRandom + Base64 |
| Hashed storage | ✅ | BCrypt |
| Scope enforcement | ✅ | Pattern matching |
| Rate limiting | ✅ | Per minute/hour/day |
| Usage logging | ✅ | All requests logged |
| Inactive key rejection | ✅ | isActive + expiresAt check |
| IP whitelisting | ✅ | Optional IP restrictions |
| API secret support | ✅ | Optional secret validation |

---

## ✅ Summary

### All Requirements Met:
1. ✅ API key filter implemented (`ApiKeyAuthenticationFilter`)
2. ✅ Client resolver implemented (`ApiClientResolver`)
3. ✅ Scope validator implemented (`ApiScopeValidator`)
4. ✅ Example secured endpoint (`VendorOrderController`)
5. ✅ API keys generated securely
6. ✅ API keys stored as hash (BCrypt)
7. ✅ Scopes enforced per endpoint
8. ✅ Rate limiting implemented
9. ✅ Usage logging per client
10. ✅ Inactive keys rejected
11. ✅ `Authorization: Api-Key {key}` format supported
12. ✅ No JWT for API clients (separate authentication)
13. ✅ Not mixed with customer/admin auth

### Production Ready:
- ✅ Secure key generation
- ✅ Hashed storage
- ✅ Scope-based authorization
- ✅ Rate limiting
- ✅ Comprehensive logging
- ✅ Error handling
- ✅ Clean, maintainable code

---

## 📚 Files Reference

- **Filter:** `src/main/java/com/tiktel/ttelgo/security/ApiKeyAuthenticationFilter.java`
- **Client Resolver:** `src/main/java/com/tiktel/ttelgo/security/ApiClientResolver.java`
- **Scope Validator:** `src/main/java/com/tiktel/ttelgo/security/ApiScopeValidator.java`
- **Service:** `src/main/java/com/tiktel/ttelgo/apikey/application/ApiKeyService.java`
- **Example Endpoint:** `src/main/java/com/tiktel/ttelgo/order/api/VendorOrderController.java`

---

**Status: ✅ IMPLEMENTATION COMPLETE AND VERIFIED**

All API client authentication code is production-ready, secure, and follows best practices.

