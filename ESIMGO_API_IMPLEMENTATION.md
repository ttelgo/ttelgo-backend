# eSIMGo API Implementation

This document describes the implemented eSIMGo API integrations in the TTelGo backend.

## Configuration

The eSIMGo API configuration is stored in `application-dev.yml`:

```yaml
esimgo:
  api:
    endpoint: https://api.esim-go.com/v2.4
    key: 2iBA_znWaTXMT5O1Q04n5uSX5i45jC2W-hDNgweR
```

## Implemented APIs

### 1. Plan Management APIs

#### List All Bundles
- **Endpoint:** `GET /api/plans/bundles`
- **Description:** Retrieves all available eSIM bundles
- **Response:** List of bundles with details

**Example Request:**
```bash
GET http://localhost:8080/api/plans/bundles
```

#### List Bundles by Country
- **Endpoint:** `GET /api/plans/bundles/country?countryIso=GB`
- **Description:** Retrieves bundles available for a specific country
- **Query Parameters:**
  - `countryIso` (required): ISO country code (e.g., GB, US, AD)

**Example Request:**
```bash
GET http://localhost:8080/api/plans/bundles/country?countryIso=GB
```

#### Get Bundle Details
- **Endpoint:** `GET /api/plans/bundles/{bundleName}`
- **Description:** Retrieves detailed information about a specific bundle
- **Path Parameters:**
  - `bundleName` (required): Bundle name (e.g., esim_1GB_7D_GB_V2)

**Example Request:**
```bash
GET http://localhost:8080/api/plans/bundles/esim_1GB_7D_GB_V2
```

### 2. eSIM Management APIs

#### Activate Bundle (Create Order)
- **Endpoint:** `POST /api/esims/activate`
- **Description:** Creates an order to activate an eSIM bundle
- **Request Body:**
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

**Example Request:**
```bash
POST http://localhost:8080/api/esims/activate
Content-Type: application/json

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

**Response:**
```json
{
  "order": [
    {
      "esims": [
        {
          "iccid": "8943108167000619644",
          "matchingId": "J1ZAA-YUGIM-DBR1I-AG9HF",
          "smdpAddress": "rsp-3104.idemia.io"
        }
      ],
      "type": "bundle",
      "item": "esim_1GB_7D_GB_V2",
      "iccids": ["8943108167000619644"],
      "quantity": 1,
      "subTotal": 1.36,
      "pricePerUnit": 1.36,
      "allowReassign": false
    }
  ],
  "total": 1.36
}
```

#### Get QR Code
- **Endpoint:** `GET /api/esims/qr/{matchingId}`
- **Description:** Retrieves the QR code for an eSIM using the matching ID
- **Path Parameters:**
  - `matchingId` (required): Matching ID from the order response

**Example Request:**
```bash
GET http://localhost:8080/api/esims/qr/J1ZAA-YUGIM-DBR1I-AG9HF
```

**Response:**
```json
{
  "qrCode": "...",
  "matchingId": "J1ZAA-YUGIM-DBR1I-AG9HF",
  "iccid": "8943108167000619644"
}
```

#### Get Order Details
- **Endpoint:** `GET /api/esims/orders/{orderId}`
- **Description:** Retrieves details of a specific order
- **Path Parameters:**
  - `orderId` (required): Order ID from the create order response

**Example Request:**
```bash
GET http://localhost:8080/api/esims/orders/c85ab1ac-bb0f-4537-857c-3f0f81acea9d
```

## Architecture

### Integration Layer
- **EsimGoClient**: Handles all HTTP communication with eSIMGo API
- **EsimGoConfig**: Manages API endpoint and key configuration
- **DTOs**: Request/Response data transfer objects for eSIMGo API

### Application Layer
- **PlanService**: Business logic for plan/bundle operations
- **EsimService**: Business logic for eSIM operations

### API Layer
- **PlanController**: REST endpoints for plan management
- **EsimController**: REST endpoints for eSIM management

## Testing

You can test the APIs using:
1. **Postman** - Import the endpoints
2. **Swagger UI** - http://localhost:8080/swagger-ui.html
3. **curl** commands

## Notes

- All requests to eSIMGo API include the `X-API-Key` header
- The API key is configured in `application-dev.yml`
- For production, use environment variables for the API key
- Database tables are not created yet (as requested)

