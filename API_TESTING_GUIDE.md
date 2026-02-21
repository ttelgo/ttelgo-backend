# API Testing Guide - Complete Flow

This guide provides URLs and examples for testing all 5 API endpoints in the eSIM activation flow.

## Base URLs

- **Localhost:** `http://localhost:8080`
- **Live Server:** `https://ttelgo.com`

**Note:** All APIs are currently **public** (no authentication required).

---

## 1. Listing Bundles

Get all available eSIM bundles.

### URLs

**Localhost:**
```
GET http://localhost:8080/api/v1/bundles
```

**Live Server:**
```
GET https://ttelgo.com/api/v1/bundles
```

### Query Parameters (Optional)
- `page` - Page number (default: 0)
- `size` - Items per page (default: 50)
- `sort` - Sort field and direction (default: "name,asc")
- `countryIso` - Filter by country ISO code (e.g., "GB", "US")
- `type` - Filter by type: "local", "regional", or "global"
- `search` - Search term for description

### Example Requests

**Get first 10 bundles:**
```
GET http://localhost:8080/api/v1/bundles?size=10
```

**Get bundles for a specific country:**
```
GET http://localhost:8080/api/v1/bundles?countryIso=GB
```

**Get bundles with pagination:**
```
GET http://localhost:8080/api/v1/bundles?page=0&size=20
```

### Example Response
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "bundles": [
      {
        "name": "esim_1GB_7D_GB_V2",
        "description": "eSIM, 1GB, 7 Days, United Kingdom, V2",
        "countries": [...],
        "dataAmount": 1000,
        "duration": 7,
        "price": 1.36,
        "billingType": "FixedCost"
      }
    ]
  },
  "meta": {
    "pagination": {
      "page": 0,
      "size": 10,
      "totalElements": 10,
      "totalPages": 1
    }
  }
}
```

---

## 2. Selecting Bundles (Get Bundle Details)

Get detailed information about a specific bundle.

### URLs

**Localhost:**
```
GET http://localhost:8080/api/v1/bundles/{bundleName}
```

**Live Server:**
```
GET https://ttelgo.com/api/v1/bundles/{bundleName}
```

### Path Parameters
- `bundleName` (required) - The bundle name from the listing (e.g., "esim_1GB_7D_GB_V2")

### Example Requests

**Localhost:**
```
GET http://localhost:8080/api/v1/bundles/esim_1GB_7D_GB_V2
```

**Live Server:**
```
GET https://ttelgo.com/api/v1/bundles/esim_1GB_7D_GB_V2
```

### Example Response
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "name": "esim_1GB_7D_GB_V2",
    "description": "eSIM, 1GB, 7 Days, United Kingdom, V2",
    "countries": [
      {
        "name": "United Kingdom",
        "region": "Europe",
        "iso": "GB"
      }
    ],
    "dataAmount": 1000,
    "duration": 7,
    "autostart": true,
    "unlimited": false,
    "roamingEnabled": false,
    "imageUrl": "https://esimgo-cms-images-prod.s3.eu-west-1.amazonaws.com/...",
    "price": 1.36,
    "billingType": "FixedCost"
  }
}
```

---

## 3. Request for Activation (Create Order)

Create an eSIM order to activate a bundle.

### URLs

**Localhost:**
```
POST http://localhost:8080/api/v1/esims/orders
```

**Live Server:**
```
POST https://ttelgo.com/api/v1/esims/orders
```

### Headers
```
Content-Type: application/json
```

### Request Body
```json
{
  "type": "transaction",
  "assign": true,
  "order": [
    {
      "type": "bundle",
      "item": "esim_1GB_7D_GB_V2",
      "quantity": 1,
      "allowReassign": false
    }
  ]
}
```

### Request Body Fields
- `type` (string, required) - `"transaction"` to create the order
- `assign` (boolean, required) - `true` to auto-create eSIM and assign bundle
- `order` (array, required) - Array of order items
  - `type` (string, required) - `"bundle"`
  - `item` (string, required) - Bundle name from step 1
  - `quantity` (integer, required) - Number of bundles to order
  - `allowReassign` (boolean, optional) - `true` to auto-create new ICCID on mismatch

### Example Request (cURL)
```bash
curl -X POST https://ttelgo.com/api/v1/esims/orders \
  -H "Content-Type: application/json" \
  -d '{
    "type": "transaction",
    "assign": true,
    "order": [
      {
        "type": "bundle",
        "item": "esim_1GB_7D_GB_V2",
        "quantity": 1,
        "allowReassign": false
      }
    ]
  }'
```

### Example Response
```json
{
  "order": [
    {
      "esims": [
        {
          "iccid": "8943108167000855917",
          "matchingId": "2bda1476-a32d-4ad2-8a11-b975b6437fc3",
          "smdpAddress": "rsp-3104.idemia.io"
        }
      ],
      "type": "bundle",
      "item": "esim_1GB_7D_GB_V2",
      "iccids": ["8943108167000855917"],
      "quantity": 1,
      "subTotal": 1.36,
      "pricePerUnit": 1.36,
      "allowReassign": false
    }
  ],
  "total": 1.36,
  "currency": "USD",
  "status": "completed",
  "statusMessage": "Order completed successfully",
  "orderReference": "ORD-1234567890",
  "createdDate": "2026-01-04T10:30:00Z",
  "assigned": true
}
```

**Important:** Save the following from the response:
- `orderReference` - This is the **reference ID** (step 4)
- `matchingId` - This is needed for getting the QR code (step 5)
- `iccid` - The eSIM card identifier

---

## 4. Get Reference ID

The reference ID is returned in the response from step 3 (Create Order).

### Reference ID Location

From the activation response, extract:
```json
{
  "orderReference": "ORD-1234567890"  // <-- This is the reference ID
}
```

**Alternative:** If `orderReference` is not present, you can use:
- The `matchingId` from `order[0].esims[0].matchingId`
- Or the first `iccid` from `order[0].esims[0].iccid`

### Example: Extracting Reference ID

**JavaScript:**
```javascript
const response = await fetch('https://ttelgo.com/api/v1/esims/orders', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    type: 'transaction',
    assign: true,
    order: [{
      type: 'bundle',
      item: 'esim_1GB_7D_GB_V2',
      quantity: 1,
      allowReassign: false
    }]
  })
});

const data = await response.json();
const orderReference = data.orderReference; // Reference ID
const matchingId = data.order[0].esims[0].matchingId; // For QR code
console.log('Order Reference:', orderReference);
console.log('Matching ID:', matchingId);
```

---

## 5. Request QR Code

Get the QR code for eSIM activation using the matching ID from step 3.

### URLs

**Localhost:**
```
GET http://localhost:8080/api/v1/esims/{matchingId}/qr
```

**Live Server:**
```
GET https://ttelgo.com/api/v1/esims/{matchingId}/qr
```

### Path Parameters
- `matchingId` (required) - The matching ID from the activation response (step 3)

### Example Requests

**Localhost:**
```
GET http://localhost:8080/api/v1/esims/2bda1476-a32d-4ad2-8a11-b975b6437fc3/qr
```

**Live Server:**
```
GET https://ttelgo.com/api/v1/esims/2bda1476-a32d-4ad2-8a11-b975b6437fc3/qr
```

### Example Request (cURL)
```bash
curl -X GET "https://ttelgo.com/api/v1/esims/2bda1476-a32d-4ad2-8a11-b975b6437fc3/qr"
```

### Example Response
```json
{
  "qrCode": "LPA:1$rsp-3104.idemia.io$...",
  "matchingId": "2bda1476-a32d-4ad2-8a11-b975b6437fc3",
  "iccid": "8943108167000855917"
}
```

**Note:** The `qrCode` field contains the LPA (Local Profile Assistant) string that can be converted to a QR code image for scanning with a device.

---

## Complete Testing Flow Example

### Step-by-Step Testing

1. **List bundles:**
   ```
   GET https://ttelgo.com/api/v1/bundles?size=5
   ```
   → Save a bundle name (e.g., "esim_1GB_7D_GB_V2")

2. **Get bundle details:**
   ```
   GET https://ttelgo.com/api/v1/bundles/esim_1GB_7D_GB_V2
   ```

3. **Create order:**
   ```
   POST https://ttelgo.com/api/v1/esims/orders
   Body: {
     "type": "transaction",
     "assign": true,
     "order": [{
       "type": "bundle",
       "item": "esim_1GB_7D_GB_V2",
       "quantity": 1,
       "allowReassign": false
     }]
   }
   ```
   → Save `orderReference` and `matchingId`

4. **Get reference ID:**
   → Already in step 3 response: `orderReference`

5. **Get QR code:**
   ```
   GET https://ttelgo.com/api/v1/esims/{matchingId}/qr
   ```
   → Use the `matchingId` from step 3

---

## Postman Collection

You can import these endpoints into Postman:

1. **List Bundles**
   - Method: `GET`
   - URL: `{{baseUrl}}/api/v1/bundles?size=10`

2. **Get Bundle Details**
   - Method: `GET`
   - URL: `{{baseUrl}}/api/v1/bundles/esim_1GB_7D_GB_V2`

3. **Create Order**
   - Method: `POST`
   - URL: `{{baseUrl}}/api/v1/esims/orders`
   - Body (raw JSON):
     ```json
     {
       "type": "transaction",
       "assign": true,
       "order": [{
         "type": "bundle",
         "item": "esim_1GB_7D_GB_V2",
         "quantity": 1,
         "allowReassign": false
       }]
     }
     ```

4. **Get QR Code**
   - Method: `GET`
   - URL: `{{baseUrl}}/api/v1/esims/{{matchingId}}/qr`
   - Variables: Set `matchingId` from step 3 response

---

## Environment Variables for Postman

Create a Postman environment with:
- `baseUrl`: `https://ttelgo.com` (for live) or `http://localhost:8080` (for local)
- `matchingId`: (set dynamically from step 3 response)

---

## Troubleshooting

### Empty Bundles Response
- Check if eSIM-Go API is accessible
- Verify `ESIMGO_API_KEY` is set correctly
- Check server logs: `sudo journalctl -u ttelgo-backend -f`

### Order Creation Fails
- Verify bundle name exists (from step 1)
- Check that `type` is set to `"transaction"` (not `"validate"`)
- Ensure `assign` is `true` to auto-create eSIM

### QR Code Not Found
- Verify `matchingId` is correct (from step 3)
- Check that order was successfully created
- Ensure eSIM was assigned (`assigned: true` in response)

---

## Quick Reference - All URLs

### Localhost
1. `GET http://localhost:8080/api/v1/bundles`
2. `GET http://localhost:8080/api/v1/bundles/{bundleName}`
3. `POST http://localhost:8080/api/v1/esims/orders`
4. (Reference ID from step 3 response)
5. `GET http://localhost:8080/api/v1/esims/{matchingId}/qr`

### Live Server
1. `GET https://ttelgo.com/api/v1/bundles`
2. `GET https://ttelgo.com/api/v1/bundles/{bundleName}`
3. `POST https://ttelgo.com/api/v1/esims/orders`
4. (Reference ID from step 3 response)
5. `GET https://ttelgo.com/api/v1/esims/{matchingId}/qr`

