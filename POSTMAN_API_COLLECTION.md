# TTelGo Backend - Postman API Collection

## Base Configuration

**Base URL:** `http://localhost:8080`

**Headers (Common):**
```json
{
  "Content-Type": "application/json",
  "Accept": "application/json"
}
```

---

## 1. 🏥 Health & Monitoring APIs (No Auth Required)

### 1.1 Health Check
```http
GET http://localhost:8080/actuator/health
```

**Expected Response:**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

### 1.2 Application Info
```http
GET http://localhost:8080/actuator/info
```

### 1.3 Metrics
```http
GET http://localhost:8080/actuator/metrics
```

### 1.4 Database Health
```http
GET http://localhost:8080/actuator/health/db
```

---

## 2. 📚 Catalogue APIs (Public - B2C)

### 2.1 List All Bundles
```http
GET http://localhost:8080/api/v1/catalogue/bundles
```

**Query Parameters (Optional):**
- `page` (default: 0)
- `size` (default: 20)
- `sortBy` (default: price)
- `sortDirection` (ASC/DESC)

**Example:**
```http
GET http://localhost:8080/api/v1/catalogue/bundles?page=0&size=10&sortBy=price&sortDirection=ASC
```

**Expected Response:**
```json
{
  "content": [
    {
      "code": "BUNDLE_US_1GB",
      "name": "USA 1GB Data Plan",
      "description": "1GB data for 7 days",
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
  "pageSize": 10,
  "totalElements": 50,
  "totalPages": 5,
  "isLast": false
}
```

### 2.2 Get Bundle Details
```http
GET http://localhost:8080/api/v1/catalogue/bundles/{bundleCode}
```

**Example:**
```http
GET http://localhost:8080/api/v1/catalogue/bundles/BUNDLE_US_1GB
```

---

## 3. 🛒 Order APIs (B2C - Requires JWT)

### 3.1 Create Order (with Idempotency)
```http
POST http://localhost:8080/api/v1/orders
```

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}
X-Idempotency-Key: order-12345-67890
```

**Request Body:**
```json
{
  "bundleCode": "BUNDLE_US_1GB",
  "quantity": 1,
  "currency": "USD",
  "customerEmail": "customer@example.com"
}
```

**Expected Response:**
```json
{
  "id": 1,
  "userId": 1,
  "bundleCode": "BUNDLE_US_1GB",
  "quantity": 1,
  "totalAmount": 9.99,
  "currency": "USD",
  "status": "CREATED",
  "esimGoOrderId": null,
  "qrCode": null,
  "smdpAddress": null,
  "activationCode": null,
  "createdAt": "2025-12-18T10:00:00",
  "updatedAt": "2025-12-18T10:00:00"
}
```

### 3.2 Get Order by ID
```http
GET http://localhost:8080/api/v1/orders/{orderId}
```

**Headers:**
```
Authorization: Bearer {JWT_TOKEN}
```

**Example:**
```http
GET http://localhost:8080/api/v1/orders/1
```

### 3.3 List User Orders
```http
GET http://localhost:8080/api/v1/orders
```

**Headers:**
```
Authorization: Bearer {JWT_TOKEN}
```

**Query Parameters:**
- `page` (default: 0)
- `size` (default: 20)

---

## 4. 💳 Payment APIs (B2C)

### 4.1 Create Payment Intent
```http
POST http://localhost:8080/api/v1/payments/create-intent
```

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}
X-Idempotency-Key: payment-12345-67890
```

**Request Body:**
```json
{
  "orderId": 1,
  "amount": 9.99,
  "currency": "USD",
  "customerEmail": "customer@example.com"
}
```

**Expected Response:**
```json
{
  "paymentId": 1,
  "paymentIntentId": "pi_1234567890abcdef",
  "clientSecret": "pi_1234567890abcdef_secret_xyz",
  "amount": 9.99,
  "currency": "USD"
}
```

---

## 5. 🏢 Vendor APIs (B2B - Requires API Key)

### 5.1 Get Vendor Catalogue
```http
GET http://localhost:8080/api/v1/vendor/catalogue/bundles
```

**Headers:**
```
X-API-Key: {VENDOR_API_KEY}
```

### 5.2 Create Vendor Order
```http
POST http://localhost:8080/api/v1/vendor/orders
```

**Headers:**
```
Content-Type: application/json
X-API-Key: {VENDOR_API_KEY}
X-Idempotency-Key: vendor-order-12345
```

**Request Body:**
```json
{
  "bundleCode": "BUNDLE_US_1GB",
  "quantity": 5,
  "currency": "USD",
  "customerReference": "CUST-REF-001"
}
```

**Expected Response:**
```json
{
  "id": 10,
  "vendorId": 1,
  "bundleCode": "BUNDLE_US_1GB",
  "quantity": 5,
  "totalAmount": 49.95,
  "currency": "USD",
  "status": "CREATED",
  "esimGoOrderId": "ESG-ORD-123456",
  "qrCode": "LPA:1$SMDP.ESIMGO.COM$ACTIVATION_CODE",
  "smdpAddress": "SMDP.ESIMGO.COM",
  "createdAt": "2025-12-18T10:00:00"
}
```

### 5.3 Get Vendor Order
```http
GET http://localhost:8080/api/v1/vendor/orders/{orderId}
```

**Headers:**
```
X-API-Key: {VENDOR_API_KEY}
```

### 5.4 List Vendor Orders
```http
GET http://localhost:8080/api/v1/vendor/orders
```

**Headers:**
```
X-API-Key: {VENDOR_API_KEY}
```

**Query Parameters:**
- `page`, `size`, `sortBy`, `sortDirection`

### 5.5 Get Vendor Balance
```http
GET http://localhost:8080/api/v1/vendor/balance
```

**Headers:**
```
X-API-Key: {VENDOR_API_KEY}
```

**Expected Response:**
```json
{
  "vendorId": 1,
  "billingMode": "PREPAID",
  "walletBalance": 1000.00,
  "creditLimit": 0.00,
  "currency": "USD",
  "status": "ACTIVE"
}
```

### 5.6 Get Ledger Entries
```http
GET http://localhost:8080/api/v1/vendor/ledger
```

**Headers:**
```
X-API-Key: {VENDOR_API_KEY}
```

**Query Parameters:**
- `page`, `size`

---

## 6. 📱 eSIM APIs

### 6.1 Get eSIM Details
```http
GET http://localhost:8080/api/v1/esims/{esimId}
```

**Headers:**
```
Authorization: Bearer {JWT_TOKEN}
```

**Example:**
```http
GET http://localhost:8080/api/v1/esims/1
```

**Expected Response:**
```json
{
  "id": 1,
  "orderId": 1,
  "iccid": "8901234567890123456",
  "matchingId": "MATCH-123",
  "smdpAddress": "SMDP.ESIMGO.COM",
  "activationCode": "ACT-CODE-123",
  "qrCode": "LPA:1$SMDP.ESIMGO.COM$ACT-CODE-123",
  "status": "ACTIVE",
  "bundleCode": "BUNDLE_US_1GB",
  "bundleName": "USA 1GB Data Plan",
  "userId": 1,
  "vendorId": null,
  "activatedAt": "2025-12-18T10:00:00",
  "createdAt": "2025-12-18T10:00:00"
}
```

### 6.2 Get eSIM by ICCID
```http
GET http://localhost:8080/api/v1/esims/iccid/{iccid}
```

**Headers:**
```
Authorization: Bearer {JWT_TOKEN}
```

### 6.3 Get eSIM QR Code
```http
GET http://localhost:8080/api/v1/esims/{esimId}/qr-code
```

**Headers:**
```
Authorization: Bearer {JWT_TOKEN}
```

---

## 7. 🔔 Webhook APIs

### 7.1 Stripe Webhook (for testing)
```http
POST http://localhost:8080/api/v1/webhooks/stripe
```

**Headers:**
```
Content-Type: application/json
Stripe-Signature: {WEBHOOK_SIGNATURE}
```

**Request Body (Example - Payment Intent Succeeded):**
```json
{
  "id": "evt_1234567890",
  "object": "event",
  "type": "payment_intent.succeeded",
  "data": {
    "object": {
      "id": "pi_1234567890",
      "object": "payment_intent",
      "amount": 999,
      "currency": "usd",
      "status": "succeeded",
      "metadata": {
        "type": "B2C_ORDER",
        "orderId": "1",
        "userId": "1",
        "paymentId": "1"
      }
    }
  }
}
```

---

## 8. 🔍 Testing Without Authentication (For Quick Testing)

Since authentication is enabled, you have a few options:

### Option 1: Disable Security Temporarily
Add to `application-dev.yml`:
```yaml
spring:
  security:
    enabled: false
```

### Option 2: Use Generated Password
Use the generated password from console for basic auth:
```
Username: user
Password: {generated-password-from-console}
```

### Option 3: Test Public Endpoints Only
These don't require auth:
- Actuator endpoints: `/actuator/*`
- Catalogue endpoints: `/api/v1/catalogue/*`

---

## 9. 📋 Sample Test Sequence

### Scenario: Complete Order Flow

1. **Get Available Bundles**
   ```http
   GET http://localhost:8080/api/v1/catalogue/bundles
   ```

2. **Create Order**
   ```http
   POST http://localhost:8080/api/v1/orders
   Headers: X-Idempotency-Key: test-order-001
   Body: { "bundleCode": "BUNDLE_US_1GB", "quantity": 1, "currency": "USD", "customerEmail": "test@example.com" }
   ```

3. **Create Payment Intent**
   ```http
   POST http://localhost:8080/api/v1/payments/create-intent
   Body: { "orderId": 1, "amount": 9.99, "currency": "USD", "customerEmail": "test@example.com" }
   ```

4. **Check Order Status**
   ```http
   GET http://localhost:8080/api/v1/orders/1
   ```

5. **Get eSIM Details**
   ```http
   GET http://localhost:8080/api/v1/esims/1
   ```

---

## 10. 🛠️ Troubleshooting

### 401 Unauthorized
- Add Basic Auth with generated password
- Or add `Authorization: Bearer {token}` header

### 403 Forbidden
- Check if endpoint requires specific role
- Verify API key for vendor endpoints

### 404 Not Found
- Verify the endpoint URL
- Check if resource exists (e.g., order ID)

### 503 Service Unavailable
- Check if external services (Redis, eSIM Go) are configured
- Verify database connection

---

## 11. 🎯 Quick Start Postman Commands

Copy these into Postman "New Request":

**Health Check:**
```
GET http://localhost:8080/actuator/health
```

**List Bundles:**
```
GET http://localhost:8080/api/v1/catalogue/bundles
```

**Get Metrics:**
```
GET http://localhost:8080/actuator/metrics
```

---

## 12. 📦 Import to Postman

Save this as `TTelGo.postman_collection.json` and import into Postman:

```json
{
  "info": {
    "name": "TTelGo Backend API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Health Check",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/actuator/health",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["actuator", "health"]
        }
      }
    },
    {
      "name": "List Bundles",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/api/v1/catalogue/bundles",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "v1", "catalogue", "bundles"]
        }
      }
    }
  ]
}
```

---

**Happy Testing! 🚀**

