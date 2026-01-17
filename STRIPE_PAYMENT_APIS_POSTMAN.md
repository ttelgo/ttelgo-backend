# Stripe Payment APIs - Postman Testing Guide

## Base URL
```
http://localhost:8080
```

## Authentication
All payment endpoints require Bearer token authentication (JWT).
Add this header to all requests:
```
Authorization: Bearer {your_jwt_token}
```

---

## API Endpoints

### 1. Create Payment Intent (Main Endpoint)

**Creates an order and Stripe PaymentIntent in one call. This is what the frontend uses.**

**Endpoint:**
```
POST /api/v1/payments/intent
```

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {jwt_token}
Idempotency-Key: {optional_unique_key}  # Optional - prevents duplicate requests
```

**Request Body:**
```json
{
  "bundleId": "esim_1GB_7D_GB_V2",
  "amount": 1.30,
  "currency": "usd",
  "customerEmail": "customer@example.com",
  "quantity": 1,
  "bundleName": "eSIM, 1GB, 7 Days, Aaland Islands, V2"
}
```

**Request Fields:**
- `bundleId` (required): The eSIM bundle ID from eSIMGo
- `amount` (optional): Payment amount - if not provided, uses bundle price
- `currency` (optional): Currency code (e.g., "usd", "eur") - defaults to bundle currency
- `customerEmail` (optional): Customer email - defaults to "customer@example.com"
- `quantity` (optional): Number of eSIMs - defaults to 1
- `bundleName` (optional): Display name for the bundle

**Response (Success - 200 OK):**
```json
{
  "success": true,
  "data": {
    "clientSecret": "pi_3Sp87mRt1tQZRnuwOQRJ5JV1_secret_xxxxxxxxxxxxx",
    "paymentIntentId": "pi_3Sp87mRt1tQZRnuwOQRJ5JV1",
    "publishableKey": "pk_test_51RISlSRt1tQZRnuwvJeTcRyyjI6Jh5tATdZsHCH76j7xFgTiNoMJ7LI6ZOn27t9gYJHkAMgw2brYUm5OtRpfEB0Z00XyOye67d",
    "orderId": 123
  }
}
```

**Response Fields:**
- `clientSecret`: Used by frontend Stripe Elements to confirm payment
- `paymentIntentId`: Stripe PaymentIntent ID
- `publishableKey`: Stripe publishable key for frontend
- `orderId`: Internal order ID created

---

### 2. Confirm Payment

**Called after client-side Stripe payment confirmation. Mainly for logging/acknowledgment.**

**Endpoint:**
```
POST /api/v1/payments/confirm?paymentIntentId={payment_intent_id}
```

**Headers:**
```
Authorization: Bearer {jwt_token}
```

**Query Parameters:**
- `paymentIntentId` (required): The Stripe PaymentIntent ID (e.g., `pi_3Sp87mRt1tQZRnuwOQRJ5JV1`)

**Example:**
```
POST /api/v1/payments/confirm?paymentIntentId=pi_3Sp87mRt1tQZRnuwOQRJ5JV1
```

**Response (Success - 200 OK):**
```json
{
  "success": true,
  "data": null
}
```

**Note:** Payment is already confirmed on Stripe side. This endpoint just acknowledges the confirmation. The webhook handles actual payment status updates.

---

### 3. Create Payment Intent for Existing Order

**Creates a PaymentIntent for an order that was already created.**

**Endpoint:**
```
POST /api/v1/payments/intents/orders
```

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {jwt_token}
Idempotency-Key: {optional_unique_key}
```

**Request Body:**
```json
{
  "orderId": 123,
  "amount": 1.30,
  "currency": "usd",
  "customerEmail": "customer@example.com"
}
```

**Request Fields:**
- `orderId` (required): Existing order ID
- `amount` (required): Payment amount
- `currency` (optional): Currency code - defaults to "USD"
- `customerEmail` (optional): Customer email

**Response (Success - 200 OK):**
```json
{
  "success": true,
  "data": {
    "clientSecret": "pi_3Sp87mRt1tQZRnuwOQRJ5JV1_secret_xxxxxxxxxxxxx",
    "paymentIntentId": "pi_3Sp87mRt1tQZRnuwOQRJ5JV1",
    "publishableKey": "pk_test_51RISlSRt1tQZRnuwvJeTcRyyjI6Jh5tATdZsHCH76j7xFgTiNoMJ7LI6ZOn27t9gYJHkAMgw2brYUm5OtRpfEB0Z00XyOye67d",
    "orderId": 123
  }
}
```

---

### 4. Activate eSIM After Payment

**Activates the eSIM bundle after payment is confirmed.**

**Endpoint:**
```
POST /api/v1/orders/{orderId}/esims
```

**Headers:**
```
Authorization: Bearer {jwt_token}
```

**Path Parameters:**
- `orderId` (required): The order ID returned from payment intent creation

**Example:**
```
POST /api/v1/orders/123/esims
```

**Response (Success - 200 OK):**
```json
{
  "orderReference": "ORD123456",
  "id": "ORD123456",
  "order": [
    {
      "orderReference": "ORD123456",
      "esims": [
        {
          "iccid": "89012345678901234567",
          "matchingId": "MATCH123",
          "qrCode": "LPA:1$your-domain.com$...",
          "activationCode": "..."
        }
      ]
    }
  ]
}
```

---

### 5. Stripe Webhook (For Testing)

**Receives webhook events from Stripe. Usually called by Stripe, not directly.**

**Endpoint:**
```
POST /api/v1/webhooks/stripe
```

**Headers:**
```
Content-Type: application/json
Stripe-Signature: t=1234567890,v1=signature_here
```

**Note:** Webhooks are usually tested using Stripe CLI:
```bash
stripe listen --forward-to http://localhost:8080/api/v1/webhooks/stripe
```

**Response (Success - 200 OK):**
```json
{
  "success": true,
  "data": "Webhook processed successfully"
}
```

---

## Postman Collection Setup

### 1. Create New Environment

Create a new environment with these variables:
```json
{
  "base_url": "http://localhost:8080",
  "jwt_token": "your_jwt_token_here",
  "payment_intent_id": "",
  "order_id": ""
}
```

### 2. Test Flow

#### Step 1: Get JWT Token (if needed)
Use your authentication endpoint to get a JWT token first.

#### Step 2: Create Payment Intent
1. Method: `POST`
2. URL: `{{base_url}}/api/v1/payments/intent`
3. Headers:
   - `Authorization: Bearer {{jwt_token}}`
   - `Content-Type: application/json`
4. Body (raw JSON):
```json
{
  "bundleId": "esim_1GB_7D_GB_V2",
  "amount": 1.30,
  "currency": "usd",
  "customerEmail": "test@example.com",
  "quantity": 1,
  "bundleName": "eSIM, 1GB, 7 Days, Aaland Islands, V2"
}
```
5. Save `paymentIntentId` and `orderId` from response to environment variables

#### Step 3: Confirm Payment
1. Method: `POST`
2. URL: `{{base_url}}/api/v1/payments/confirm?paymentIntentId={{payment_intent_id}}`
3. Headers:
   - `Authorization: Bearer {{jwt_token}}`

**Note:** In real flow, payment is confirmed client-side with Stripe. This endpoint just acknowledges it.

#### Step 4: Activate eSIM
1. Method: `POST`
2. URL: `{{base_url}}/api/v1/orders/{{order_id}}/esims`
3. Headers:
   - `Authorization: Bearer {{jwt_token}}`

---

## Common Test Scenarios

### Scenario 1: Complete Payment Flow
1. Create Payment Intent → Get `orderId` and `paymentIntentId`
2. (In real app, confirm payment with Stripe client-side)
3. Confirm Payment → Acknowledge confirmation
4. Activate eSIM → Get QR code and eSIM details

### Scenario 2: Test with Different Bundle IDs
Try different bundle IDs:
- `esim_1GB_7D_GB_V2`
- `esim_5GB_30D_GB_V2`
- Other bundle IDs from your eSIMGo account

### Scenario 3: Test Amount Validation
Try creating payment intent with:
- Invalid amount (negative, zero)
- Invalid currency
- Missing required fields

---

## Error Responses

### 400 Bad Request
```json
{
  "success": false,
  "message": "Validation error message",
  "errors": [
    {
      "field": "bundleId",
      "message": "Bundle ID is required"
    }
  ]
}
```

### 401 Unauthorized
```json
{
  "success": false,
  "message": "Unauthorized"
}
```

### 500 Internal Server Error
```json
{
  "success": false,
  "message": "Internal server error message"
}
```

---

## Important Notes

1. **Authentication Required**: All endpoints require JWT token in Authorization header
2. **Idempotency Key**: Optional header to prevent duplicate payment intent creation
3. **Payment Confirmation**: Real payment confirmation happens client-side with Stripe Elements. The `/confirm` endpoint just acknowledges it.
4. **Webhooks**: Stripe sends webhooks automatically. Use Stripe CLI for local testing.
5. **Order ID**: Save the `orderId` from payment intent response - needed for eSIM activation

---

## Testing Checklist

- [ ] Create payment intent successfully
- [ ] Get clientSecret in response
- [ ] Confirm payment (acknowledgment)
- [ ] Activate eSIM after payment
- [ ] Verify error handling for invalid requests
- [ ] Test with different bundle IDs
- [ ] Test with different amounts/currencies

---

## Stripe Test Keys (Already Configured)

- **Publishable Key**: `pk_test_51RISlSRt1tQZRnuwvJeTcRyyjI6Jh5tATdZsHCH76j7xFgTiNoMJ7LI6ZOn27t9gYJHkAMgw2brYUm5OtRpfEB0Z00XyOye67d`
- **Secret Key**: Configured in backend `application-dev.yml`

## Test Cards (For Stripe Dashboard/Client Testing)

- **Success**: `4242 4242 4242 4242`
- **Decline**: `4000 0000 0000 0002`
- **3D Secure**: `4000 0025 0000 3155`

Expiry: Any future date (e.g., `12/34`)
CVC: Any 3 digits (e.g., `123`)
ZIP: Any 5 digits (e.g., `12345`)

