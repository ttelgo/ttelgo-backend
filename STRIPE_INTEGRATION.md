# Stripe Payment Integration

## Overview
This document describes the Stripe payment integration that replaces the direct eSIMGo payment flow. Now, payments are processed through Stripe before activating eSIMs with eSIMGo.

## Architecture

### Payment Flow
1. **Frontend**: User selects a plan and proceeds to checkout
2. **Backend**: Creates a payment intent via Stripe API
3. **Frontend**: User enters payment details using Stripe Elements
4. **Stripe**: Processes payment and sends webhook
5. **Backend**: Confirms payment and activates eSIM with eSIMGo
6. **Frontend**: Displays success and QR code

### Key Components

#### Backend
- **StripeConfig**: Configures Stripe API keys from environment variables
- **StripeClient**: Wraps Stripe API calls (create payment intent, retrieve, confirm)
- **PaymentService**: Business logic for payment processing
- **PaymentController**: REST endpoints for payment operations
- **StripeWebhookHandler**: Handles Stripe webhook events
- **EsimService**: Updated to activate eSIM only after payment confirmation

#### Database
- **payments table**: Stores payment records with Stripe payment intent IDs
- **orders table**: Updated to track payment status separately from order status

## API Endpoints

### Create Payment Intent
```
POST /api/v1/payments/intent
Content-Type: application/json

{
  "amount": 29.99,
  "currency": "usd",
  "bundleId": "esim_1GB_7D_GB_V2",
  "bundleName": "UK eSIM 1GB 7 Days",
  "quantity": 1,
  "customerEmail": "customer@example.com"
}
```

Response:
```json
{
  "success": true,
  "data": {
    "clientSecret": "pi_xxx_secret_xxx",
    "paymentIntentId": "pi_xxx",
    "publishableKey": "pk_test_xxx",
    "orderId": 123
  }
}
```

### Confirm Payment
```
POST /api/v1/payments/confirm?paymentIntentId=pi_xxx
```

### Activate eSIM After Payment
```
POST /api/v1/esims/activate-after-payment?orderId=123
```

### Stripe Webhook
```
POST /api/v1/webhooks/stripe
Headers:
  Stripe-Signature: t=xxx,v1=xxx
```

## Configuration

### Environment Variables
Add to `application.yml` or environment:
```yaml
stripe:
  secret:
    key: ${STRIPE_SECRET_KEY}
  publishable:
    key: ${STRIPE_PUBLISHABLE_KEY}
  webhook:
    secret: ${STRIPE_WEBHOOK_SECRET}
```

### Test Keys (Current)
- **Publishable Key**: `pk_test_xxxxxxxxxxxxx` (Set via environment variable STRIPE_PUBLISHABLE_KEY)
- **Secret Key**: `sk_test_xxxxxxxxxxxxx` (Set via environment variable STRIPE_SECRET_KEY)

**⚠️ IMPORTANT: Never commit actual API keys to git. Use environment variables instead.**

## Frontend Integration

### Required Steps
1. Install Stripe.js: `npm install @stripe/stripe-js @stripe/react-stripe-js`
2. Create payment intent on checkout page load
3. Initialize Stripe Elements with client secret
4. Handle payment confirmation
5. Call activate-after-payment endpoint
6. Display success and QR code

### Example Frontend Code
```typescript
import { loadStripe } from '@stripe/stripe-js';
import { Elements, CardElement, useStripe, useElements } from '@stripe/react-stripe-js';

const stripePromise = loadStripe('pk_test_...');

// In checkout component:
const { clientSecret, orderId } = await createPaymentIntent({
  bundleId: plan.bundleId,
  amount: plan.price,
  currency: 'usd'
});

// After payment confirmation:
await confirmPayment(paymentIntentId);
await activateEsimAfterPayment(orderId);
```

## Testing

### Test Cards
Use Stripe test cards:
- **Success**: `4242 4242 4242 4242`
- **Decline**: `4000 0000 0000 0002`
- **3D Secure**: `4000 0025 0000 3155`

### Webhook Testing
Use Stripe CLI:
```bash
stripe listen --forward-to http://localhost:8080/api/v1/webhooks/stripe
```

## Migration Notes

### Database
The `payments` table is created via Flyway migration `V7__create_payments_table.sql`.

### Backward Compatibility
The legacy `/api/esims/activate` endpoint has been removed. Use the v1 payment flow instead.

## Security Considerations

1. **API Keys**: Never expose secret keys in frontend code
2. **Webhook Verification**: Always verify webhook signatures in production
3. **Amount Validation**: Always validate amounts server-side
4. **Order Status**: Only activate eSIM after payment confirmation

## Next Steps

1. Update frontend checkout page to use Stripe Elements
2. Configure webhook endpoint in Stripe Dashboard
3. Test end-to-end payment flow
4. Switch to production Stripe keys when ready

