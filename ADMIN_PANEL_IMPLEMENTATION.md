# Admin Panel & API Key System - Implementation Complete

## ✅ Backend Implementation

### 1. Database Schema
- **Migration:** `V6__create_api_keys_and_usage_tracking.sql`
- **Tables Created:**
  - `api_keys` - Stores API key information, rate limits, IP whitelisting
  - `api_usage_logs` - Tracks all API requests with detailed metrics
  - `api_rate_limit_tracking` - Manages rate limiting per time window

### 2. Domain Entities
- `ApiKey` - API key entity with validation
- `ApiUsageLog` - Request logging entity
- `ApiRateLimitTracking` - Rate limit tracking entity

### 3. Services & Controllers
- **ApiKeyService** - Complete CRUD operations, validation, usage stats
- **ApiKeyController** - REST endpoints for API key management
- **Enhanced AdminService** - Comprehensive dashboard statistics
- **AdminController** - Dashboard and admin endpoints

### 4. Security & Authentication
- **ApiKeyAuthenticationFilter** - Validates API keys, checks IP whitelisting
- **ApiUsageLoggingInterceptor** - Logs all API requests
- **SecurityConfig** - Updated to include API key authentication
- **WebConfig** - Added interceptor for usage logging

### 5. API Endpoints

#### Admin Dashboard
- `GET /api/admin/dashboard` - Get comprehensive dashboard statistics

#### API Key Management
- `POST /api/admin/api-keys` - Create new API key
- `GET /api/admin/api-keys` - List all API keys
- `GET /api/admin/api-keys/{id}` - Get API key details
- `PUT /api/admin/api-keys/{id}` - Update API key
- `DELETE /api/admin/api-keys/{id}` - Delete API key
- `POST /api/admin/api-keys/{id}/regenerate` - Regenerate API key
- `GET /api/admin/api-keys/{id}/usage` - Get usage statistics

## ✅ Frontend Implementation

### 1. Admin Module Structure
```
src/modules/admin/
├── pages/
│   ├── AdminDashboard.tsx      # Main dashboard with stats
│   └── ApiKeyManagement.tsx    # API key CRUD interface
└── services/
    ├── admin.service.ts        # Dashboard API calls
    └── apikey.service.ts        # API key management calls
```

### 2. Pages Created

#### Admin Dashboard (`/admin` or `/admin/dashboard`)
- **Features:**
  - User statistics (total, new users today/week/month)
  - Order statistics (total, pending, completed, cancelled)
  - Revenue breakdown (total, today, this week, this month)
  - eSIM statistics (total, active, activated today/week)
  - API statistics (total keys, active keys, requests, response time)
  - Recent orders list
  - Recent users list
  - Top API keys by usage

#### API Key Management (`/admin/api-keys`)
- **Features:**
  - Create new API keys with configurable rate limits
  - List all API keys with status and usage
  - Edit API key settings
  - Regenerate API keys (shows new key once)
  - Delete API keys
  - View detailed usage statistics per key
  - Display top endpoints, status codes, daily usage

### 3. API Client Updates
- **API Key Support:** Frontend can now authenticate using API keys
- **Headers:** `X-API-Key` and `X-API-Secret` (optional)
- **Storage:** API keys stored in `localStorage` as `api_key` and `api_secret`
- **Fallback:** Falls back to JWT token authentication if no API key

### 4. Type Definitions
All admin-related types added to `src/shared/types/index.ts`:
- `AdminDashboardStats`
- `ApiKey`, `CreateApiKeyRequest`, `UpdateApiKeyRequest`
- `ApiUsageStats`, `EndpointUsage`, `StatusCodeCount`, `DailyUsage`

## 🔐 API Key Authentication

### How It Works
1. **API Key Generation:** Admin creates API key through admin panel
2. **Key Storage:** API key and secret stored in database
3. **Request Authentication:** Client sends `X-API-Key` header (and optionally `X-API-Secret`)
4. **Validation:** Backend validates key, checks expiration, IP whitelist, rate limits
5. **Usage Logging:** All requests logged with metrics (response time, status, endpoint)

### Rate Limiting
- Configurable per key:
  - Per minute (default: 60)
  - Per hour (default: 1000)
  - Per day (default: 10000)

### IP Whitelisting
- Optional IP address restrictions per API key
- Supports wildcard (`*`) for all IPs

### Scopes (Future)
- Endpoint-level access control (prepared in schema)

## 📊 Usage Analytics

### Tracked Metrics
- Total requests per API key
- Requests by time period (today, week, month)
- Average response time
- Error rate and count
- Top endpoints by usage
- Status code distribution
- Daily usage trends

### Access
- View usage stats per API key in admin panel
- Filter by time period (default: 30 days)

## 🚀 Usage

### For Frontend
1. **Set API Key:**
   ```javascript
   localStorage.setItem('api_key', 'your-api-key-here')
   localStorage.setItem('api_secret', 'your-api-secret-here') // Optional
   ```

2. **All API calls** will automatically use the API key if set

### For External Customers
1. **Get API Key:** Contact admin or use admin panel
2. **Use in Requests:**
   ```bash
   curl -H "X-API-Key: your-api-key" \
        -H "X-API-Secret: your-api-secret" \
        https://api.ttelgo.com/api/plans/bundles
   ```

## 📝 Routes

### Frontend Routes
- `/admin` - Admin dashboard
- `/admin/dashboard` - Admin dashboard (alias)
- `/admin/api-keys` - API key management
- `/admin/blog` - Blog management (existing)
- `/admin/faq` - FAQ management (existing)

### Backend Routes
- `/api/admin/dashboard` - Dashboard stats
- `/api/admin/api-keys` - API key CRUD
- `/api/admin/api-keys/{id}/usage` - Usage statistics

## 🎨 UI Features

- **Modern Design:** Clean, card-based layout
- **Responsive:** Works on desktop and mobile
- **Real-time Stats:** Dashboard updates on load
- **Interactive Tables:** Sortable, filterable API key list
- **Modal Dialogs:** Usage stats, new key display
- **Color Coding:** Status indicators (active/inactive, order status)
- **Form Validation:** Client-side validation for API key creation

## 🔄 Next Steps (Future Enhancements)

1. **API Key Scopes:** Implement endpoint-level access control
2. **Advanced Rate Limiting:** Token bucket algorithm
3. **Webhooks:** API key usage notifications
4. **API Documentation:** Auto-generated docs per API key
5. **Usage Alerts:** Notify when rate limits approached
6. **Billing Integration:** Track API usage for billing

## 📌 Notes

- **Admin endpoints are currently open** (no authentication required)
- **API key authentication works** alongside JWT authentication
- **Usage logging is automatic** for all API key requests
- **Frontend can use API keys** by setting them in localStorage
- **All admin features accessible** via frontend at `/admin/*` routes

