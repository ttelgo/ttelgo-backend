# TTelGo Backend API - Quick Reference

## Base URL
```
http://localhost:8080
```

---

## 📋 All Available APIs

### Plan/Bundle APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/plans/bundles` | List all bundles |
| `GET` | `/api/plans/bundles/country?countryIso={code}` | List bundles by country |
| `GET` | `/api/plans/bundles/{bundleName}` | Get bundle details |

### eSIM APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/esims/activate` | Activate bundle (create order) |
| `GET` | `/api/esims/qr/{matchingId}` | Get QR code |
| `GET` | `/api/esims/orders/{orderId}` | Get order details |

### Health Check APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/health/db` | Database health check |

---

## Quick Examples

### 1. Get All Bundles
```javascript
fetch('http://localhost:8080/api/plans/bundles')
  .then(res => res.json())
  .then(data => console.log(data.bundles));
```

### 2. Get Bundles by Country
```javascript
fetch('http://localhost:8080/api/plans/bundles/country?countryIso=GB')
  .then(res => res.json())
  .then(data => console.log(data.bundles));
```

### 3. Activate Bundle
```javascript
fetch('http://localhost:8080/api/esims/activate', {
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
})
.then(res => res.json())
.then(data => {
  const matchingId = data.order[0].esims[0].matchingId;
  console.log('Matching ID:', matchingId);
});
```

### 4. Get QR Code
```javascript
const matchingId = 'your-matching-id';
fetch(`http://localhost:8080/api/esims/qr/${matchingId}`)
  .then(res => res.json())
  .then(data => console.log('QR Code:', data.qrCode));
```

---

## Response Status Codes

- `200` - Success
- `400` - Bad Request
- `403` - Forbidden
- `404` - Not Found
- `500` - Internal Server Error
- `503` - Service Unavailable

---

## Important Notes

- ✅ No authentication required (currently)
- ✅ All endpoints return JSON
- ✅ Use `Content-Type: application/json` for POST requests
- ✅ Save `matchingId` from activate response to get QR code

---

For detailed documentation, see `API_DOCUMENTATION.md`

