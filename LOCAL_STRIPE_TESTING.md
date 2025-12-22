# 🧪 Local Stripe Testing Guide

## ✅ Configuration Status

**Backend:**
- ✅ Stripe keys configured in `application-dev.yml`
- ✅ Local webhook secret configured: `whsec_914baea8785a68fbbb148c821389c2e0c0d44d752b97fb151fd92c84cb42f7d7`
- ✅ Webhook handler ready at `/api/v1/webhooks/stripe`

**Frontend:**
- ✅ Stripe packages installed (`@stripe/stripe-js`, `@stripe/react-stripe-js`)
- ✅ Payment service configured
- ✅ Checkout page integrated with Stripe

---

## 🚀 Step-by-Step Local Testing

### Step 1: Start Backend Server

**Open PowerShell Window #1:**
```powershell
cd D:\tiktel\full-stack\ttelgo-backend
mvn spring-boot:run
```

**Wait until you see:**
```
Started TtelgoApplication in X.XXX seconds
```

**Keep this window open!**

---

### Step 2: Start Stripe Webhook Listener

**Open PowerShell Window #2 (NEW window):**
```powershell
cd D:\tiktel\full-stack\ttelgo-backend
stripe listen --forward-to http://localhost:8080/api/v1/webhooks/stripe
```

**You should see:**
```
> Ready! Your webhook signing secret is whsec_914baea8785a68fbbb148c821389c2e0c0d44d752b97fb151fd92c84cb42f7d7
```

**Keep this window open!** The listener must stay running.

---

### Step 3: Start Frontend

**Open PowerShell Window #3 (NEW window):**
```powershell
cd D:\tiktel\full-stack\ttelgo-frontend
npm run dev
```

**Wait until you see:**
```
  VITE v5.x.x  ready in XXX ms
  ➜  Local:   http://localhost:5173/
```

**Keep this window open!**

---

### Step 4: Test Payment Flow

1. **Open browser:** Go to `http://localhost:5173`
2. **Navigate to checkout:**
   - Select a plan from the shop
   - Click "Buy Now" or proceed to checkout
3. **Fill billing information:**
   - Email: `test@example.com`
   - Name: `Test User`
   - Address: Any test address
4. **Enter test card:**
   - **Card Number:** `4242 4242 4242 4242`
   - **Expiry:** `12/34` (any future date)
   - **CVC:** `123` (any 3 digits)
   - **ZIP:** `12345` (any 5 digits)
5. **Click "Pay Now" or "Complete Payment"**

---

### Step 5: Verify Payment

**Check Backend Logs (Window #1):**
- You should see: `Payment intent created`
- You should see: `Webhook received: payment_intent.succeeded`
- You should see: `Payment confirmed`
- You should see: `eSIM activated`

**Check Webhook Listener (Window #2):**
- You should see webhook events being received
- Events like: `payment_intent.succeeded`

**Check Frontend:**
- Should redirect to success page
- Should show QR code for eSIM

---

## 🧪 Test Cards

Use these Stripe test cards:

| Card Number | Result | Use Case |
|------------|--------|----------|
| `4242 4242 4242 4242` | ✅ Success | Normal payment |
| `4000 0000 0000 0002` | ❌ Decline | Test declined payment |
| `4000 0025 0000 3155` | 🔐 3D Secure | Test 3D Secure flow |
| `4000 0000 0000 9995` | ❌ Insufficient Funds | Test insufficient funds |

**Test Details (for all cards):**
- **Expiry:** Any future date (e.g., `12/34`)
- **CVC:** Any 3 digits (e.g., `123`)
- **ZIP:** Any 5 digits (e.g., `12345`)

---

## 🔍 Troubleshooting

### Backend Not Starting
- Check if port 8080 is already in use
- Check database connection
- Check logs for errors

### Webhook Listener Not Working
- Make sure backend is running first
- Check that port 8080 is accessible
- Try running `stripe login` again if needed

### Payment Fails
- Check browser console for errors
- Check backend logs for payment intent creation
- Verify Stripe keys are correct in `application-dev.yml`

### Webhook Not Received
- Make sure webhook listener is running
- Check backend logs for webhook endpoint
- Verify webhook secret matches in config

---

## 📊 What to Check

### Backend Logs Should Show:
```
✅ Payment intent created: pi_xxx
✅ Webhook received: payment_intent.succeeded
✅ Payment confirmed: pi_xxx
✅ Order updated: orderId=xxx
✅ eSIM activated: orderId=xxx
```

### Webhook Listener Should Show:
```
✅ payment_intent.succeeded [200]
```

### Frontend Should:
- ✅ Show payment form
- ✅ Process payment successfully
- ✅ Redirect to success page
- ✅ Display QR code

---

## ✅ Success Checklist

- [ ] Backend running on port 8080
- [ ] Webhook listener running and showing `Ready!`
- [ ] Frontend running on port 5173
- [ ] Can access checkout page
- [ ] Payment intent created successfully
- [ ] Payment processed with test card
- [ ] Webhook received in listener
- [ ] Payment confirmed in backend logs
- [ ] eSIM activated
- [ ] QR code displayed

---

## 🎯 Quick Test Command

To test webhook connection quickly:
```powershell
# In a new PowerShell window (while webhook listener is running)
stripe trigger payment_intent.succeeded
```

This sends a test webhook to your backend without making a real payment.

---

**Ready to test? Start with Step 1!** 🚀

