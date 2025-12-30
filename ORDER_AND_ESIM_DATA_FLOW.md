# Order and eSIM Data Flow - Complete Guide

This document explains how data flows from frontend checkout to database insertion for Orders and eSIMs.

## 📋 Overview

When a user purchases an eSIM from the frontend:
1. Frontend calls `/api/esims/activate` endpoint
2. Backend calls eSIMGo API to activate the bundle
3. **Backend saves Order and Esim entities to PostgreSQL database**
4. Response is returned to frontend

---

## 🔄 Complete Data Flow

### Step 1: Frontend Checkout (`Checkout.tsx`)

**Location:** `ttelgo-frontend/src/modules/checkout/pages/Checkout.tsx`

**Process:**
1. User fills billing information and clicks "Complete Purchase"
2. Frontend calls `checkoutService.createOrder()`
3. `checkoutService` extracts `userId` from localStorage (if user is logged in)
4. Creates `ActivateBundleRequest` with:
   - `type: 'transaction'`
   - `assign: true`
   - `userId: <user_id>` (if logged in, otherwise undefined)
   - `order: [{ type: 'bundle', item: bundleId, quantity: 1, ... }]`

**Code:**
```typescript
// ttelgo-frontend/src/modules/checkout/services/checkout.service.ts
const activateRequest: ActivateBundleRequest = {
  type: 'transaction',
  assign: true,
  userId: userId, // From localStorage if logged in
  order: [{
    type: 'bundle',
    item: bundleId,
    quantity: 1,
    allowReassign: false,
  }],
}
```

---

### Step 2: Backend API Endpoint (`EsimController`)

**Location:** `ttelgo-backend/src/main/java/com/tiktel/ttelgo/esim/api/EsimController.java`

**Endpoint:** `POST /api/esims/activate`

**Process:**
1. Receives `ActivateBundleRequest` from frontend
2. Calls `esimService.activateBundle(request)`

**Code:**
```java
@PostMapping("/activate")
public ResponseEntity<ActivateBundleResponse> activateBundle(
        @RequestBody ActivateBundleRequest request) {
    ActivateBundleResponse response = esimService.activateBundle(request);
    return ResponseEntity.ok(response);
}
```

---

### Step 3: eSIM Service - eSIMGo API Call (`EsimService`)

**Location:** `ttelgo-backend/src/main/java/com/tiktel/ttelgo/esim/application/EsimService.java`

**Process:**
1. Maps `ActivateBundleRequest` to `CreateOrderRequest` (eSIMGo format)
2. Calls `esimGoClient.createOrder()` → **External eSIMGo API**
3. Receives `CreateOrderResponse` from eSIMGo
4. **NEW:** Saves Order and Esim entities to database
5. Returns `ActivateBundleResponse` to controller

**Code:**
```java
@Transactional
public ActivateBundleResponse activateBundle(ActivateBundleRequest request) {
    // Step 1: Create order with eSIMGo API
    CreateOrderRequest createOrderRequest = mapToCreateOrderRequest(request);
    CreateOrderResponse esimGoResponse = esimGoClient.createOrder(createOrderRequest);
    
    // Step 2: Save Order and Esim entities to database
    if (esimGoResponse != null && "success".equalsIgnoreCase(esimGoResponse.getStatus())) {
        saveOrderAndEsimsToDatabase(request, esimGoResponse);
    }
    
    // Step 3: Return response
    return mapToActivateBundleResponse(esimGoResponse);
}
```

---

### Step 4: Database Insertion (`saveOrderAndEsimsToDatabase`)

**Location:** `ttelgo-backend/src/main/java/com/tiktel/ttelgo/esim/application/EsimService.java`

**Process:**

#### 4.1. Create Order Entity

**Data Mapped:**
- `orderReference` → From eSIMGo `orderReference` (UUID)
- `userId` → From request (if provided)
- `bundleId` → From request `order[0].item`
- `bundleName` → From request `order[0].item`
- `quantity` → From request `order[0].quantity`
- `unitPrice` → From eSIMGo `order[0].pricePerUnit`
- `totalAmount` → From eSIMGo `total`
- `currency` → From eSIMGo `currency`
- `status` → `OrderStatus.COMPLETED`
- `paymentStatus` → `PaymentStatus.SUCCESS`
- `matchingId` → From eSIMGo `order[0].esims[0].matchingId`
- `iccid` → From eSIMGo `order[0].esims[0].iccid`
- `smdpAddress` → From eSIMGo `order[0].esims[0].smdpAddress`
- `esimgoOrderId` → From eSIMGo `orderReference`

**Code:**
```java
Order order = Order.builder()
    .orderReference(esimGoResponse.getOrderReference())
    .userId(request.getUserId())
    .bundleId(bundleId)
    .bundleName(bundleName)
    .quantity(quantity)
    .unitPrice(BigDecimal.valueOf(pricePerUnit))
    .totalAmount(BigDecimal.valueOf(total))
    .currency(currency)
    .status(OrderStatus.COMPLETED)
    .paymentStatus(PaymentStatus.SUCCESS)
    .matchingId(matchingId)
    .iccid(iccid)
    .smdpAddress(smdpAddress)
    .esimgoOrderId(esimGoResponse.getOrderReference())
    .build();

Order savedOrder = orderRepositoryPort.save(order);
```

**Database Table:** `orders`

#### 4.2. Create Esim Entity(ies)

**For each eSIM in eSIMGo response:**

**Data Mapped:**
- `esimUuid` → Generated UUID (for QR code endpoint)
- `orderId` → From saved `Order.id`
- `userId` → From request (if provided)
- `bundleId` → From request
- `bundleName` → From request
- `matchingId` → From eSIMGo `esims[].matchingId`
- `iccid` → From eSIMGo `esims[].iccid`
- `smdpAddress` → From eSIMGo `esims[].smdpAddress`
- `status` → `EsimStatus.PROVISIONED`
- `esimgoOrderId` → From eSIMGo `orderReference`

**Code:**
```java
for (CreateOrderResponse.OrderDetail orderDetail : esimGoResponse.getOrder()) {
    if (orderDetail.getEsims() != null) {
        for (CreateOrderResponse.EsimInfo esimInfo : orderDetail.getEsims()) {
            Esim esim = Esim.builder()
                .esimUuid(UUID.randomUUID().toString())
                .orderId(savedOrder.getId())
                .userId(request.getUserId())
                .bundleId(bundleId)
                .bundleName(bundleName)
                .matchingId(esimInfo.getMatchingId())
                .iccid(esimInfo.getIccid())
                .smdpAddress(esimInfo.getSmdpAddress())
                .status(EsimStatus.PROVISIONED)
                .esimgoOrderId(esimGoResponse.getOrderReference())
                .build();
            
            esimRepositoryPort.save(esim);
        }
    }
}
```

**Database Table:** `esims`

---

## 📊 Database Tables

### `orders` Table

**Created by:** Hibernate/JPA (no explicit migration, but table exists)

**Key Columns:**
- `id` (BIGSERIAL PRIMARY KEY)
- `order_reference` (VARCHAR, UNIQUE) - UUID from eSIMGo
- `user_id` (BIGINT) - User who placed order (nullable)
- `bundle_id` (VARCHAR) - Bundle identifier
- `bundle_name` (VARCHAR) - Bundle name
- `quantity` (INTEGER)
- `unit_price` (DECIMAL)
- `total_amount` (DECIMAL)
- `currency` (VARCHAR)
- `status` (ENUM) - PENDING, PROCESSING, COMPLETED, CANCELLED, REFUNDED, FAILED
- `payment_status` (ENUM) - PENDING, SUCCESS, FAILED, REFUNDED, PARTIALLY_REFUNDED
- `matching_id` (VARCHAR) - From eSIMGo
- `iccid` (VARCHAR) - From eSIMGo
- `smdp_address` (VARCHAR) - From eSIMGo
- `esimgo_order_id` (VARCHAR) - eSIMGo order reference
- `created_at` (TIMESTAMP) - Auto-set by `@PrePersist`
- `updated_at` (TIMESTAMP) - Auto-set by `@PrePersist` and `@PreUpdate`

### `esims` Table

**Created by:** Migration files `V3__add_activated_at_to_esims.sql` and `V4__update_esims_table_schema.sql`

**Key Columns:**
- `id` (BIGSERIAL PRIMARY KEY)
- `esim_uuid` (VARCHAR, UNIQUE) - Generated UUID for QR code endpoint
- `order_id` (BIGINT) - Foreign key to `orders.id`
- `user_id` (BIGINT) - User who owns eSIM (nullable)
- `bundle_id` (VARCHAR) - Bundle identifier
- `bundle_name` (VARCHAR) - Bundle name
- `matching_id` (VARCHAR, UNIQUE) - From eSIMGo
- `iccid` (VARCHAR, UNIQUE) - From eSIMGo
- `smdp_address` (VARCHAR) - From eSIMGo
- `activation_code` (TEXT) - QR code data (can be fetched later)
- `status` (ENUM) - PENDING, PROVISIONED, ACTIVATED, EXPIRED, CANCELLED
- `activated_at` (TIMESTAMP) - When eSIM was activated
- `expires_at` (TIMESTAMP) - When eSIM expires
- `data_amount` (INTEGER) - Data amount in MB
- `data_used` (INTEGER) - Data used in MB
- `duration_days` (INTEGER) - Validity in days
- `country_iso` (VARCHAR) - Country ISO code
- `country_name` (VARCHAR) - Country name
- `esimgo_order_id` (VARCHAR) - eSIMGo order reference
- `created_at` (TIMESTAMP) - Auto-set by `@PrePersist`
- `updated_at` (TIMESTAMP) - Auto-set by `@PrePersist` and `@PreUpdate`

---

## 🧪 Testing the Flow

### Prerequisites

1. **Backend running** on `http://localhost:8080`
2. **Frontend running** on `http://localhost:5173`
3. **PostgreSQL database** accessible
4. **eSIMGo API key** configured in `application.yml`

### Test Steps

1. **Start Backend:**
   ```bash
   cd ttelgo-backend
   ./mvnw spring-boot:run
   ```

2. **Start Frontend:**
   ```bash
   cd ttelgo-frontend
   npm run dev
   ```

3. **Navigate to Checkout:**
   - Go to `http://localhost:5173/shop` or any plan page
   - Select a plan and click "Buy Now"
   - Fill in billing information
   - Click "Complete Purchase"

4. **Verify Database Records:**

   **Check Orders:**
   ```sql
   SELECT * FROM orders ORDER BY created_at DESC LIMIT 1;
   ```

   **Check eSIMs:**
   ```sql
   SELECT * FROM esims ORDER BY created_at DESC LIMIT 1;
   ```

   **Check Order-ESIM Relationship:**
   ```sql
   SELECT o.id, o.order_reference, o.user_id, o.total_amount, 
          e.id as esim_id, e.matching_id, e.iccid, e.status
   FROM orders o
   LEFT JOIN esims e ON e.order_id = o.id
   ORDER BY o.created_at DESC
   LIMIT 5;
   ```

5. **Verify Admin APIs:**

   **Get All Orders:**
   ```bash
   curl http://localhost:8080/api/admin/orders
   ```

   **Get All eSIMs:**
   ```bash
   curl http://localhost:8080/api/admin/esims
   ```

---

## 🔍 Troubleshooting

### Issue: Orders not appearing in database

**Check:**
1. Is eSIMGo API returning `status: "success"`?
2. Check backend logs for errors in `saveOrderAndEsimsToDatabase()`
3. Verify database connection is working
4. Check if `@Transactional` is working (transaction might be rolling back)

### Issue: eSIMs not appearing in database

**Check:**
1. Does eSIMGo response contain `order[].esims[]` array?
2. Check if `esimGoResponse.getOrder()` is not null
3. Verify `esimRepositoryPort.save()` is being called
4. Check for unique constraint violations (matchingId, iccid must be unique)

### Issue: userId is null

**Check:**
1. Is user logged in? Check localStorage for `user` object
2. Does user object have `id` field?
3. `userId` is optional - orders can be created without userId (guest checkout)

---

## 📝 Notes

1. **userId is Optional:** Orders and eSIMs can be created without userId (for guest checkout). When authentication is fully implemented, userId will be extracted from JWT token.

2. **Transaction Management:** The `@Transactional` annotation ensures that if database save fails, the entire operation is rolled back.

3. **eSIMGo Order Reference:** The `orderReference` from eSIMGo is stored in both `orders.order_reference` and `orders.esimgo_order_id` for reference.

4. **Status Mapping:**
   - Order status: `COMPLETED` (after successful eSIMGo activation)
   - Payment status: `SUCCESS` (eSIMGo accepted the order)
   - eSIM status: `PROVISIONED` (eSIM is provisioned and ready)

5. **QR Code:** QR code is not stored immediately. It can be fetched later using the `matchingId` via `/api/esims/qr/{matchingId}` endpoint.

---

## 🔄 Future Enhancements

1. **Extract userId from JWT:** When authentication is fully implemented, extract userId from JWT token instead of request body.

2. **Payment Integration:** Currently, payment is handled by eSIMGo. Future: Integrate payment gateway (Stripe, PayPal) and update `payment_status` based on payment result.

3. **Webhook Support:** Add webhook endpoint to receive updates from eSIMGo about order status changes.

4. **QR Code Caching:** Store QR code in `esims.activation_code` after fetching it.

5. **Error Handling:** Add retry logic for database saves and better error messages.

