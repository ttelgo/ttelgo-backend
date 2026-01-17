# TTelGo Backend API Documentation

## Base URL
```
Development: http://localhost:8080
Production: [To be configured]
```

## Authentication
Currently, all APIs are publicly accessible (no authentication required). This will change in future updates.

---

## 📋 Table of Contents
1. [Plan/Bundle Management APIs](#planbundle-management-apis)
2. [eSIM Management APIs](#esim-management-apis)
3. [Health Check APIs](#health-check-apis)
4. [Error Handling](#error-handling)

---

## Plan/Bundle Management APIs

### 1. List All Bundles

Get a list of all available eSIM bundles.

**Endpoint:** `GET /api/plans/bundles`

**Request:**
- **Method:** `GET`
- **Headers:** None required
- **Body:** None

**Response:**
- **Status Code:** `200 OK`
- **Content-Type:** `application/json`

**Response Body:**
```json
{
  "bundles": [
    {
      "name": "esim_1GB_7D_GB_V2",
      "description": "1GB Data, 7 Days, United Kingdom",
      "countries": [
        {
          "name": "United Kingdom",
          "region": "Europe",
          "iso": "GB"
        }
      ],
      "dataAmount": 1024,
      "duration": 7,
      "autostart": true,
      "unlimited": false,
      "roamingEnabled": true,
      "imageUrl": "https://example.com/image.jpg",
      "price": 1.36,
      "group": ["data", "europe"],
      "billingType": "prepaid",
      "potentialSpeeds": ["4G", "5G"]
    }
  ]
}
```

**Example Request (JavaScript/Fetch):**
```javascript
const response = await fetch('http://localhost:8080/api/plans/bundles');
const data = await response.json();
console.log(data.bundles);
```

**Example Request (Axios):**
```javascript
const response = await axios.get('http://localhost:8080/api/plans/bundles');
console.log(response.data.bundles);
```

---

### 2. List Bundles by Country

Get bundles available for a specific country.

**Endpoint:** `GET /api/plans/bundles/country`

**Request:**
- **Method:** `GET`
- **Headers:** None required
- **Query Parameters:**
  - `countryIso` (required, string): ISO country code (e.g., "GB", "US", "AD", "FR")

**Response:**
- **Status Code:** `200 OK`
- **Content-Type:** `application/json`
- **Response Body:** Same structure as "List All Bundles"

**Example Request:**
```
GET http://localhost:8080/api/plans/bundles/country?countryIso=GB
```

**Example Request (JavaScript/Fetch):**
```javascript
const countryIso = 'GB';
const response = await fetch(
  `http://localhost:8080/api/plans/bundles/country?countryIso=${countryIso}`
);
const data = await response.json();
```

**Example Request (Axios):**
```javascript
const response = await axios.get('http://localhost:8080/api/plans/bundles/country', {
  params: { countryIso: 'GB' }
});
```

---

### 3. Get Bundle Details

Get detailed information about a specific bundle.

**Endpoint:** `GET /api/plans/bundles/{bundleName}`

**Request:**
- **Method:** `GET`
- **Headers:** None required
- **Path Parameters:**
  - `bundleName` (required, string): Bundle name (e.g., "esim_1GB_7D_GB_V2")

**Response:**
- **Status Code:** `200 OK`
- **Content-Type:** `application/json`

**Response Body:**
```json
{
  "name": "esim_1GB_7D_GB_V2",
  "description": "1GB Data, 7 Days, United Kingdom",
  "countries": [
    {
      "name": "United Kingdom",
      "region": "Europe",
      "iso": "GB"
    }
  ],
  "dataAmount": 1024,
  "duration": 7,
  "autostart": true,
  "unlimited": false,
  "roamingEnabled": true,
  "imageUrl": "https://example.com/image.jpg",
  "price": 1.36,
  "group": ["data", "europe"],
  "billingType": "prepaid",
  "potentialSpeeds": ["4G", "5G"]
}
```

**Example Request:**
```
GET http://localhost:8080/api/plans/bundles/esim_1GB_7D_GB_V2
```

**Example Request (JavaScript/Fetch):**
```javascript
const bundleName = 'esim_1GB_7D_GB_V2';
const response = await fetch(
  `http://localhost:8080/api/plans/bundles/${bundleName}`
);
const bundle = await response.json();
```

---

## eSIM Management APIs

### 4. Create Order (eSIM-Go Compatible)

Create an eSIM order directly - matches eSIM-Go API structure exactly for testing.

**Endpoint:** `POST /api/v1/esims/orders`

**Note:** This endpoint is compatible with the eSIM-Go API format and can be used for direct testing.

---

### 4.1. Activate Bundle (Legacy Endpoint)

Create an order to activate an eSIM bundle.

**Endpoint:** `POST /api/v1/esim-orders`

**Request:**
- **Method:** `POST`
- **Headers:**
  - `Content-Type: application/json`
- **Body:** JSON object

**Request Body:**
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

**Request Body Fields:**
- `type` (string, required): `"transaction"` or `"validate"` (use "transaction" to actually create the order)
- `assign` (boolean, required): `true` to auto-create eSIM and assign bundle
- `order` (array, required): Array of order items
  - `type` (string, required): `"bundle"`
  - `item` (string, required): Bundle name from the bundles list
  - `quantity` (integer, required): Number of bundles to order
  - `allowReassign` (boolean, optional): `true` to auto-create new ICCID on mismatch

**Response:**
- **Status Code:** `200 OK`
- **Content-Type:** `application/json`

**Response Body:**
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
  "total": 1.36
}
```

**Important Notes:**
- Save the `matchingId` from the response - you'll need it to get the QR code
- Save the `orderId` (if provided) or use the first `matchingId` to track the order
- The `iccid` is the eSIM card identifier
- The `smdpAddress` is the SM-DP+ server address for eSIM activation

**Example Request (Postman/curl):**
```bash
curl -X POST http://localhost:8080/api/v1/esims/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
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

**Example Request (JavaScript/Fetch):**
```javascript
const response = await fetch('http://localhost:8080/api/v1/esims/orders', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer YOUR_JWT_TOKEN'
  },
  body: JSON.stringify({
    type: 'transaction',
    assign: true,
    order: [
      {
        type: 'bundle',
        item: 'esim_1GB_7D_GB_V2',
        quantity: 1,
        allowReassign: false
      }
    ]
  })
});

const data = await response.json();
const matchingId = data.order[0].esims[0].matchingId;
console.log('Matching ID:', matchingId);
```

**Example Request (Axios):**
```javascript
const response = await axios.post('http://localhost:8080/api/v1/esims/orders', {
  type: 'transaction',
  assign: true,
  order: [
    {
      type: 'bundle',
      item: 'esim_1GB_7D_GB_V2',
      quantity: 1,
      allowReassign: false
    }
  ]
});

const matchingId = response.data.order[0].esims[0].matchingId;
```

---

### 5. Get QR Code

Get the QR code for eSIM activation using the matching ID.

**Endpoint:** `GET /api/esims/qr/{matchingId}`

**Request:**
- **Method:** `GET`
- **Headers:** None required
- **Path Parameters:**
  - `matchingId` (required, string): Matching ID from the activate bundle response

**Response:**
- **Status Code:** `200 OK`
- **Content-Type:** `application/json`

**Response Body:**
```json
{
  "qrCode": "LPA:1$rsp-3104.idemia.io$...",
  "matchingId": "2bda1476-a32d-4ad2-8a11-b975b6437fc3",
  "iccid": "8943108167000855917"
}
```

**Response Fields:**
- `qrCode` (string): QR code string for eSIM activation (can be used to generate QR code image)
- `matchingId` (string): The matching ID used in the request
- `iccid` (string): The eSIM card identifier

**Example Request:**
```
GET http://localhost:8080/api/esims/qr/2bda1476-a32d-4ad2-8a11-b975b6437fc3
```

**Example Request (JavaScript/Fetch):**
```javascript
const matchingId = '2bda1476-a32d-4ad2-8a11-b975b6437fc3';
const response = await fetch(
  `http://localhost:8080/api/esims/qr/${matchingId}`
);
const data = await response.json();
console.log('QR Code:', data.qrCode);
// Use data.qrCode to generate QR code image for user to scan
```

**Example Request (Axios):**
```javascript
const matchingId = '2bda1476-a32d-4ad2-8a11-b975b6437fc3';
const response = await axios.get(`http://localhost:8080/api/esims/qr/${matchingId}`);
console.log('QR Code:', response.data.qrCode);
```

**Frontend Implementation Tip:**
Use a QR code library (e.g., `qrcode.react` or `react-qr-code`) to display the QR code:
```javascript
import QRCode from 'qrcode.react';

// After getting the QR code from API
<QRCode value={data.qrCode} size={256} />
```

---

### 6. Get Order Details

Get details of an order by order ID.

**Endpoint:** `GET /api/esims/orders/{orderId}`

**Request:**
- **Method:** `GET`
- **Headers:** None required
- **Path Parameters:**
  - `orderId` (required, string): Order ID from the activate bundle response

**Response:**
- **Status Code:** `200 OK`
- **Content-Type:** `application/json`
- **Response Body:** Same structure as "Activate Bundle" response

**Example Request:**
```
GET http://localhost:8080/api/esims/orders/your-order-id-here
```

**Example Request (JavaScript/Fetch):**
```javascript
const orderId = 'your-order-id-here';
const response = await fetch(
  `http://localhost:8080/api/esims/orders/${orderId}`
);
const orderDetails = await response.json();
```

---

## Health Check APIs

### 7. Database Health Check

Check the database connection status.

**Endpoint:** `GET /api/health/db`

**Request:**
- **Method:** `GET`
- **Headers:** None required
- **Body:** None

**Response:**
- **Status Code:** `200 OK` (if database is connected) or `503 Service Unavailable` (if database is down)
- **Content-Type:** `application/json`

**Success Response (200 OK):**
```json
{
  "status": "UP",
  "database": "PostgreSQL",
  "version": "16.0",
  "url": "jdbc:postgresql://localhost:5432/ttelgo_dev",
  "username": "postgres",
  "driverName": "PostgreSQL JDBC Driver",
  "driverVersion": "42.7.0",
  "connected": true
}
```

**Error Response (503 Service Unavailable):**
```json
{
  "status": "DOWN",
  "error": "Connection refused",
  "connected": false
}
```

**Example Request:**
```javascript
const response = await fetch('http://localhost:8080/api/health/db');
const health = await response.json();
if (health.connected) {
  console.log('Database is connected');
} else {
  console.error('Database connection failed');
}
```

---

## Error Handling

### Common HTTP Status Codes

- **200 OK:** Request successful
- **400 Bad Request:** Invalid request parameters or body
- **403 Forbidden:** Access denied (should not occur with current configuration)
- **404 Not Found:** Resource not found (e.g., bundle name doesn't exist)
- **500 Internal Server Error:** Server error (check server logs)
- **503 Service Unavailable:** Service temporarily unavailable (e.g., database down)

### Error Response Format

All errors follow this format:

```json
{
  "timestamp": "2025-11-24T13:08:33.894+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Bundle not found",
  "path": "/api/plans/bundles/invalid-bundle-name"
}
```

### Error Handling Example (React)

```javascript
async function fetchBundles() {
  try {
    const response = await fetch('http://localhost:8080/api/plans/bundles');
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to fetch bundles');
    }
    
    const data = await response.json();
    return data.bundles;
  } catch (error) {
    console.error('Error fetching bundles:', error);
    // Handle error in UI
    throw error;
  }
}
```

---

## Complete Workflow Example

Here's a complete example of the eSIM activation workflow:

```javascript
// Step 1: Get all bundles
const bundlesResponse = await fetch('http://localhost:8080/api/plans/bundles');
const bundlesData = await bundlesResponse.json();
const selectedBundle = bundlesData.bundles[0]; // User selects a bundle

// Step 2: Activate the bundle
const activateResponse = await fetch('http://localhost:8080/api/esims/activate', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    type: 'transaction',
    assign: true,
    order: [
      {
        type: 'bundle',
        item: selectedBundle.name,
        quantity: 1,
        allowReassign: false
      }
    ]
  })
});

const activateData = await activateResponse.json();
const matchingId = activateData.order[0].esims[0].matchingId;

// Step 3: Get QR code
const qrResponse = await fetch(
  `http://localhost:8080/api/esims/qr/${matchingId}`
);
const qrData = await qrResponse.json();

// Step 4: Display QR code to user
console.log('QR Code:', qrData.qrCode);
// Display QR code in UI for user to scan
```

---

## TypeScript Type Definitions

For TypeScript projects, here are the type definitions:

```typescript
// Bundle Types
interface CountryDto {
  name: string;
  region: string;
  iso: string;
}

interface BundleDto {
  name: string;
  description: string;
  countries: CountryDto[];
  dataAmount: number;
  duration: number;
  autostart: boolean;
  unlimited: boolean;
  roamingEnabled: boolean;
  imageUrl: string;
  price: number;
  group: string[];
  billingType: string;
  potentialSpeeds: string[];
}

interface ListBundlesResponse {
  bundles: BundleDto[];
}

// eSIM Types
interface OrderItem {
  type: string;
  item: string;
  quantity: number;
  allowReassign: boolean;
}

interface ActivateBundleRequest {
  type: 'transaction' | 'validate';
  assign: boolean;
  order: OrderItem[];
}

interface EsimInfo {
  iccid: string;
  matchingId: string;
  smdpAddress: string;
}

interface OrderDetail {
  esims: EsimInfo[];
  type: string;
  item: string;
  iccids: string[];
  quantity: number;
  subTotal: number;
  pricePerUnit: number;
  allowReassign: boolean;
}

interface ActivateBundleResponse {
  order: OrderDetail[];
  total: number;
}

interface EsimQrResponse {
  qrCode: string;
  matchingId: string;
  iccid: string;
}

// Health Check Types
interface HealthResponse {
  status: 'UP' | 'DOWN';
  database?: string;
  version?: string;
  url?: string;
  username?: string;
  driverName?: string;
  driverVersion?: string;
  connected: boolean;
  error?: string;
}
```

---

## Notes for Frontend Developers

1. **Base URL Configuration:** Store the base URL in environment variables or a config file
2. **Error Handling:** Always handle errors gracefully and show user-friendly messages
3. **Loading States:** Show loading indicators during API calls
4. **QR Code Display:** Use a QR code library to render the QR code string as an image
5. **CORS:** If you encounter CORS issues, contact the backend team to configure CORS headers
6. **Authentication:** Currently no authentication is required, but this will be added in the future
7. **Rate Limiting:** Be mindful of API rate limits (if implemented)

---

## Support

For questions or issues, contact the backend development team.

**Last Updated:** November 24, 2025

