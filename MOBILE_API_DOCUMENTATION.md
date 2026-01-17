# TTelGo eSIM API Documentation for Mobile App

**Base URL:** `https://ttelgo.com/api/v1`

**Authentication:** Currently, all APIs are **public** (no authentication required).

---

## 1. List Bundles

Get all available eSIM bundles with pagination and filtering options.

### Endpoint
```
GET /api/v1/bundles
```

### Full URL
```
https://ttelgo.com/api/v1/bundles
```

### Query Parameters (All Optional)
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | 0 | Page number (0-based) |
| `size` | integer | 50 | Number of items per page |
| `sort` | string | "name,asc" | Sort field and direction (e.g., "price,desc", "name,asc") |
| `countryIso` | string | - | Filter by country ISO code (e.g., "GB", "US", "AD") |
| `type` | string | - | Filter by type: "local", "regional", or "global" |
| `search` | string | - | Search term for bundle description |

### Example Requests

**Get first 10 bundles:**
```
GET https://ttelgo.com/api/v1/bundles?size=10
```

**Get bundles for a specific country:**
```
GET https://ttelgo.com/api/v1/bundles?countryIso=GB
```

**Get bundles with pagination:**
```
GET https://ttelgo.com/api/v1/bundles?page=0&size=20&sort=price,asc
```

**Get local bundles:**
```
GET https://ttelgo.com/api/v1/bundles?type=local
```

### Response Format
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "bundles": [
      {
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
        "imageUrl": "https://esimgo-cms-images-prod.s3.eu-west-1.amazonaws.com/United_Kingdom_...jpg",
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

### Response Fields
- `name` (string) - Bundle identifier (use this for Get Bundle Detail and Create Order)
- `description` (string) - Human-readable bundle description
- `countries` (array) - List of countries this bundle covers
  - `name` (string) - Country name
  - `region` (string) - Region (e.g., "Europe", "Asia")
  - `iso` (string) - ISO country code (e.g., "GB", "US")
- `dataAmount` (integer) - Data amount in MB
- `duration` (integer) - Validity period in days
- `autostart` (boolean) - Whether eSIM auto-starts
- `unlimited` (boolean) - Whether data is unlimited
- `roamingEnabled` (boolean) - Whether roaming is enabled
- `imageUrl` (string) - Bundle image URL
- `price` (number) - Price in USD
- `billingType` (string) - Billing type (e.g., "FixedCost")

---

## 2. Get Bundle Details

Get detailed information about a specific bundle.

### Endpoint
```
GET /api/v1/bundles/{bundleName}
```

### Full URL
```
https://ttelgo.com/api/v1/bundles/{bundleName}
```

### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `bundleName` | string | Yes | Bundle name from List Bundles API (e.g., "esim_1GB_7D_GB_V2") |

### Example Request
```
GET https://ttelgo.com/api/v1/bundles/esim_1GB_7D_GB_V2
```

### Response Format
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
    "imageUrl": "https://esimgo-cms-images-prod.s3.eu-west-1.amazonaws.com/United_Kingdom_...jpg",
    "price": 1.36,
    "billingType": "FixedCost"
  }
}
```

### Response Fields
Same as List Bundles response, but returns a single bundle object instead of an array.

---

## 3. Create Order

Create an eSIM order to activate a bundle.

### Endpoint
```
POST /api/v1/esims/orders
```

### Full URL
```
https://ttelgo.com/api/v1/esims/orders
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
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | string | Yes | Must be `"transaction"` to create the order |
| `assign` | boolean | Yes | Must be `true` to auto-create eSIM and assign bundle |
| `order` | array | Yes | Array of order items |
| `order[].type` | string | Yes | Must be `"bundle"` |
| `order[].item` | string | Yes | Bundle name from List Bundles API |
| `order[].quantity` | integer | Yes | Number of bundles to order (typically 1) |
| `order[].allowReassign` | boolean | No | `true` to auto-create new ICCID on mismatch (default: `false`) |

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

### Response Format
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "order": [
      {
        "esims": [
          {
            "iccid": "8943108167001960542",
            "matchingId": "BCCVJ-TMDA6-BWUDC-PMXUV",
            "smdpAddress": "rsp-3104.idemia.io"
          }
        ],
        "type": "bundle",
        "item": "esim_1GB_7D_GB_V2",
        "iccids": ["8943108167001960542"],
        "quantity": 1,
        "subTotal": 1.36,
        "pricePerUnit": 1.36,
        "allowReassign": false
      }
    ],
    "total": 1.36,
    "currency": "USD",
    "status": "completed",
    "statusMessage": "Order completed: 1 eSIMs assigned",
    "orderReference": "73028252-1f26-4f22-9517-4fc879dbb8fb",
    "createdDate": "2026-01-04T13:15:43.785826016Z",
    "assigned": true
  }
}
```

### Response Fields
- `order` (array) - Order items
  - `esims` (array) - eSIM information
    - `iccid` (string) - eSIM card identifier
    - `matchingId` (string) - **IMPORTANT: Use this for Get QR Code API**
    - `smdpAddress` (string) - SM-DP+ server address
  - `item` (string) - Bundle name
  - `quantity` (integer) - Quantity ordered
  - `subTotal` (number) - Subtotal for this item
  - `pricePerUnit` (number) - Price per unit
- `total` (number) - Total order amount
- `currency` (string) - Currency code (e.g., "USD")
- `status` (string) - Order status (e.g., "completed")
- `statusMessage` (string) - Status message
- `orderReference` (string) - **Order reference ID** (save this for tracking)
- `createdDate` (string) - ISO 8601 timestamp
- `assigned` (boolean) - Whether eSIM was assigned

### Important Notes
- **Save the `matchingId`** from `data.order[0].esims[0].matchingId` - you'll need it for Get QR Code
- **Save the `orderReference`** - use this to track the order
- The `iccid` is the eSIM card identifier
- Response status code: `201 Created` on success

---

## 4. Get QR Code

Get the QR code for eSIM activation using the matching ID from Create Order.

### Endpoint
```
GET /api/v1/esims/{matchingId}/qr
```

### Full URL
```
https://ttelgo.com/api/v1/esims/{matchingId}/qr
```

### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `matchingId` | string | Yes | Matching ID from Create Order response (`data.order[0].esims[0].matchingId`) |

### Example Request
```
GET https://ttelgo.com/api/v1/esims/BCCVJ-TMDA6-BWUDC-PMXUV/qr
```

### Response Format
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "qrCode": "LPA:1$rsp-3104.idemia.io$...",
    "matchingId": "BCCVJ-TMDA6-BWUDC-PMXUV",
    "iccid": "8943108167001960542"
  }
}
```

### Response Fields
- `qrCode` (string) - **LPA (Local Profile Assistant) string** - Convert this to a QR code image for scanning
- `matchingId` (string) - Matching ID (same as requested)
- `iccid` (string) - eSIM card identifier

### Important Notes
- The `qrCode` field contains an LPA string that needs to be converted to a QR code image
- Users scan this QR code with their device to install the eSIM
- The QR code format is: `LPA:1$<smdp-address>$<activation-code>`

---

## Complete Integration Flow

### Step-by-Step Example

1. **List Bundles:**
   ```
   GET https://ttelgo.com/api/v1/bundles?size=10
   ```
   → User selects a bundle (e.g., `esim_1GB_7D_GB_V2`)

2. **Get Bundle Details (Optional):**
   ```
   GET https://ttelgo.com/api/v1/bundles/esim_1GB_7D_GB_V2
   ```
   → Display detailed information to user

3. **Create Order:**
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
   → Save `matchingId` from response: `data.order[0].esims[0].matchingId`
   → Save `orderReference` from response: `data.orderReference`

4. **Get QR Code:**
   ```
   GET https://ttelgo.com/api/v1/esims/{matchingId}/qr
   ```
   → Convert `qrCode` string to QR code image
   → Display QR code to user for scanning

---

## Error Handling

### Common HTTP Status Codes
- `200 OK` - Request successful
- `201 Created` - Resource created successfully (Create Order)
- `400 Bad Request` - Invalid request (check request body format)
- `404 Not Found` - Resource not found (e.g., bundle name doesn't exist)
- `500 Internal Server Error` - Server error (retry request)

### Error Response Format
```json
{
  "success": false,
  "message": "Error message here",
  "errors": {
    "path": "/api/v1/esims/orders",
    "error": "Bad Request",
    "status": 400,
    "timestamp": "2026-01-04T13:15:43.785826016Z"
  }
}
```

### Error Handling Best Practices
1. Always check `success` field` in response
2. Display `message` to user for errors
3. Log `errors` object for debugging
4. Retry on 500 errors (with exponential backoff)
5. Validate request body before sending

---

## Testing

### Test URLs (Live Server)

1. **List Bundles:**
   ```
   https://ttelgo.com/api/v1/bundles?size=5
   ```

2. **Get Bundle Details:**
   ```
   https://ttelgo.com/api/v1/bundles/esim_1GB_7D_GB_V2
   ```

3. **Create Order:**
   ```
   POST https://ttelgo.com/api/v1/esims/orders
   ```

4. **Get QR Code:**
   ```
   https://ttelgo.com/api/v1/esims/{matchingId}/qr
   ```

### Postman Collection

You can test all APIs using Postman or any HTTP client:
- Import the collection from: `TTelGo_API_Collection.postman_collection.json`
- Set base URL to: `https://ttelgo.com/api/v1`

---

## QR Code Generation

The `qrCode` field from Get QR Code API contains an LPA string. To display it as a QR code:

### iOS (Swift)
```swift
import CoreImage

func generateQRCode(from string: String) -> UIImage? {
    let data = string.data(using: String.Encoding.ascii)
    guard let filter = CIFilter(name: "CIQRCodeGenerator") else { return nil }
    filter.setValue(data, forKey: "inputMessage")
    let transform = CGAffineTransform(scaleX: 3, y: 3)
    guard let output = filter.outputImage?.transformed(by: transform) else { return nil }
    return UIImage(ciImage: output)
}
```

### Android (Kotlin)
```kotlin
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Bitmap
import android.graphics.Color

fun generateQRCode(text: String, width: Int, height: Int): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
    return bitmap
}
```

---

## Support

For issues or questions:
- Check error messages in API responses
- Verify request body format matches examples
- Ensure bundle names are correct (case-sensitive)
- Contact backend team for API issues

---

## Changelog

- **2026-01-04**: All APIs are now public (no authentication required)
- **2026-01-04**: Fixed database constraints for order creation
- **2026-01-04**: Updated payment status constraint to support SUCCEEDED

---

**Last Updated:** January 4, 2026  
**API Version:** v1  
**Base URL:** `https://ttelgo.com/api/v1`

