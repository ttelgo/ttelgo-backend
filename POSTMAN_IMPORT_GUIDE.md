# 📦 Postman Collection Import Guide

## 🚀 Quick Import (2 Steps)

### Step 1: Import Collection
1. Open **Postman**
2. Click **Import** button (top left)
3. Select **`TTelGo_API_Collection.postman_collection.json`**
4. Click **Import**

### Step 2: Import Environment (Optional)
1. Click **Import** again
2. Select **`TTelGo_API.postman_environment.json`**
3. Click **Import**
4. Select "TTelGo Development" from environment dropdown (top right)

---

## ✅ You're Ready!

The collection includes **40+ API endpoints** organized in folders:
- ✅ Health & Monitoring (4 endpoints)
- ✅ Catalogue (2 endpoints)
- ✅ Orders (3 endpoints)
- ✅ Payments (2 endpoints)
- ✅ Vendor APIs (7 endpoints)
- ✅ eSIM Management (4 endpoints)
- ✅ Webhooks (1 endpoint)
- ✅ Admin APIs (3 endpoints)

---

## 🎯 Test Immediately (No Setup)

### Public Endpoints (Work Out of the Box):

1. **Health Check**
   - Folder: `Health & Monitoring`
   - Request: `Health Check`
   - Just click **Send**! ✅

2. **List Bundles**
   - Folder: `Catalogue (Public B2C)`
   - Request: `List All Bundles`
   - Just click **Send**! ✅

3. **Get Metrics**
   - Folder: `Health & Monitoring`
   - Request: `All Metrics`
   - Just click **Send**! ✅

---

## 🔐 For Protected Endpoints

The collection has **Basic Auth pre-configured** at the collection level:
- **Username:** `user`
- **Password:** `49a07390-33b4-442f-b013-56b39d9469f8`

This will automatically apply to all authenticated requests!

---

## 📝 Variables Included

The collection includes these variables (auto-configured):

| Variable | Value | Usage |
|----------|-------|-------|
| `{{base_url}}` | `http://localhost:8080` | All endpoints |
| `{{api_key}}` | (Set your key) | Vendor API endpoints |
| `{{jwt_token}}` | (Set your token) | User endpoints |
| `{{idempotency_key}}` | Auto-generated UUID | Order/Payment requests |

---

## 🎨 Features

### Auto-Generated Values
- ✅ **Idempotency Keys** - Automatically generates unique UUID for each request
- ✅ **Random Data** - Uses Postman's `{{$randomUUID}}` for test data

### Pre-Request Scripts
- ✅ Idempotency key generation
- ✅ Timestamp generation

### Request Examples
Each request includes:
- ✅ Proper headers
- ✅ Sample request bodies
- ✅ Query parameters
- ✅ Descriptions

---

## 🧪 Recommended Test Sequence

### 1. Health Check Flow
```
Health & Monitoring → Health Check
Health & Monitoring → Application Info
Health & Monitoring → All Metrics
```

### 2. Catalogue Flow
```
Catalogue → List All Bundles
Catalogue → Get Bundle Details
```

### 3. Complete Order Flow
```
Orders → Create Order
Payments → Create Payment Intent
Orders → Get Order by ID
eSIM Management → Get eSIM by ID
eSIM Management → Get eSIM QR Code
```

### 4. Vendor Flow (B2B)
```
Vendor APIs → Get Vendor Catalogue
Vendor APIs → Get Vendor Balance
Vendor APIs → Create Vendor Order
Vendor APIs → Get Vendor Order
Vendor APIs → Get Ledger Entries
```

---

## 🔧 Customization

### Update Base URL
1. Click on collection name: **TTelGo Backend API**
2. Go to **Variables** tab
3. Change `base_url` value (e.g., for production: `https://api.ttelgo.com`)

### Add API Key
1. Click on collection: **TTelGo Backend API**
2. Go to **Variables** tab
3. Set `api_key` value to your vendor API key

### Add JWT Token
1. Click on collection: **TTelGo Backend API**
2. Go to **Variables** tab
3. Set `jwt_token` value after successful login

---

## 📊 Sample Responses

### Health Check Response
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### List Bundles Response
```json
{
  "content": [
    {
      "code": "BUNDLE_US_1GB",
      "name": "USA 1GB Data Plan",
      "description": "1GB data valid for 7 days",
      "price": 9.99,
      "currency": "USD",
      "dataAmount": "1GB",
      "validityDays": 7,
      "countries": ["United States"],
      "countryCode": "US",
      "available": true
    }
  ],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 50,
  "totalPages": 3,
  "isLast": false
}
```

### Create Order Response
```json
{
  "id": 1,
  "userId": 1,
  "bundleCode": "BUNDLE_US_1GB",
  "quantity": 1,
  "totalAmount": 9.99,
  "currency": "USD",
  "status": "CREATED",
  "createdAt": "2025-12-18T10:00:00",
  "updatedAt": "2025-12-18T10:00:00"
}
```

---

## 🐛 Troubleshooting

### 401 Unauthorized
**Solution:** Basic auth is already configured. Make sure the password matches your console output.

### 403 Forbidden
**Solution:** Endpoint requires specific role. Check if using correct authentication (JWT vs API Key).

### 404 Not Found
**Solution:** 
- Verify base URL is `http://localhost:8080`
- Check if application is running
- Ensure endpoint path is correct

### 503 Service Unavailable
**Solution:**
- Check if application is running: `GET /actuator/health`
- Verify database is connected
- Check console logs for errors

---

## 💡 Pro Tips

1. **Use Folders** - Navigate easily through organized API groups
2. **Check Descriptions** - Each request has usage details
3. **Test Sequences** - Use Collection Runner for automated testing
4. **Save Responses** - Save example responses for documentation
5. **Environment Variables** - Switch between dev/staging/prod easily

---

## 📞 Support

If you encounter issues:
1. Check application logs in terminal
2. Verify application is running: `http://localhost:8080/actuator/health`
3. Review `POSTMAN_API_COLLECTION.md` for detailed API documentation
4. Check `APPLICATION_RUNNING_SUMMARY.md` for system status

---

**Happy Testing! 🎉**

The TTelGo Backend API is ready for comprehensive testing with Postman!

