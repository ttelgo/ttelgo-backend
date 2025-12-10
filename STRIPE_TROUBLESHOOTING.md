# Stripe Integration Troubleshooting Guide

## Current Status
✅ Backend code compiles successfully
✅ Stripe SDK properly integrated
✅ Payment endpoints created
✅ Frontend Stripe Elements integrated

## Issues Identified and Fixed

### 1. Backend Compilation Error - FIXED ✅
**Error:** `The method setMe…d for the type PaymentIntentCreateParams.Builder`

**Root Cause:** The method name was incorrect. Stripe Java SDK 24.16.0 uses `putMetadata()` method.

**Fix Applied:**
- Changed from `setMetadata()` to `putMetadata()`
- Code now compiles successfully
- **Action Required:** Restart backend server to apply changes

### 2. Frontend Multiple API Calls - FIXED ✅
**Issue:** Payment intent was being created multiple times, causing 21,000+ requests

**Root Causes:**
- useEffect dependency array included `paymentIntent`, causing infinite loops
- No debouncing on email input
- Payment intent created even for non-Stripe payment methods

**Fixes Applied:**
- Removed `paymentIntent` from useEffect dependencies
- Added 800ms debounce for email input
- Added check to only create payment intent when Stripe is selected AND email is valid
- Added state reset when switching payment methods
- Added cancellation flag to prevent multiple simultaneous requests

### 3. Loading Message Showing for All Methods - FIXED ✅
**Issue:** "Initializing payment..." showed even for Bank Transfer

**Fix Applied:**
- Loading message now only shows when `paymentMethod === 'stripe'`
- Added proper conditional rendering

## Required Actions

### 1. Restart Backend Server
The backend code has been updated and compiles successfully, but the running server needs to be restarted to load the new code.

**Steps:**
```bash
# Stop the current backend server (Ctrl+C if running in terminal)
# Then restart:
cd ttelgo-backend
mvn spring-boot:run
# OR if using systemd:
sudo systemctl restart ttelgo-backend
```

### 2. Clear Browser Cache
Clear your browser cache or use Incognito mode to ensure the frontend loads the latest code.

### 3. Test the Flow
1. Navigate to checkout page
2. Select "Credit/Debit Card" (Stripe)
3. Enter a valid email address
4. Wait for payment intent to initialize (should only happen once)
5. Enter card details using Stripe Elements
6. Complete payment

## Verification Steps

### Backend Verification
1. Check backend logs for: `Created payment intent: pi_xxx for order: xxx`
2. Verify no compilation errors in logs
3. Test endpoint directly: `POST /api/payments/intent`

### Frontend Verification
1. Open browser DevTools → Network tab
2. Filter by "Fetch/XHR"
3. Navigate to checkout page
4. Enter email address
5. Should see **only ONE** request to `/api/payments/intent`
6. Request should return 200 status with payment intent data

## Common Issues and Solutions

### Issue: "Failed to create payment intent" error
**Solution:**
- Verify Stripe API keys are correct in `application.yml`
- Check backend server is running and restarted
- Verify network connectivity to Stripe API

### Issue: Multiple payment intent requests
**Solution:**
- Clear browser cache
- Check that useEffect dependencies are correct (should NOT include `paymentIntent`)
- Verify debounce is working (800ms delay)

### Issue: Payment intent not created
**Solution:**
- Ensure email is valid format (contains @)
- Ensure "Credit/Debit Card" (Stripe) is selected
- Check browser console for errors
- Verify backend endpoint is accessible

## Stripe Test Cards

Use these test cards for testing:
- **Success:** `4242 4242 4242 4242`
- **Decline:** `4000 0000 0000 0002`
- **3D Secure:** `4000 0025 0000 3155`

**Test Details:**
- Expiry: Any future date (e.g., 12/34)
- CVC: Any 3 digits (e.g., 123)
- ZIP: Any 5 digits (e.g., 12345)

## Next Steps

1. **Restart backend server** (CRITICAL - code changes won't take effect until restart)
2. **Test payment flow** end-to-end
3. **Monitor network requests** - should see only 1 payment intent request per checkout
4. **Check backend logs** for any errors

## Support

If issues persist after restarting the backend:
1. Check backend logs for detailed error messages
2. Verify Stripe API keys are correct
3. Test Stripe API connectivity
4. Check network tab for actual error responses

