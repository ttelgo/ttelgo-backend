# ⚡ Stripe Integration - Quick Start Checklist

## 🎯 YOUR TASKS (Do These First)

### ✅ Task 1: Stripe Account Setup
- [ ] Go to https://dashboard.stripe.com/register (or login)
- [ ] Make sure you're in **Test Mode** (toggle in top right)
- [ ] Go to **Developers → API keys**
- [ ] Copy **Publishable key**: `pk_test_...`
- [ ] Copy **Secret key**: `sk_test_...`
- [ ] **Share these with me**

### ✅ Task 2: Install Stripe CLI
Choose one method:

**Option A (Easiest - Scoop):**
```powershell
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
irm get.scoop.sh | iex
scoop bucket add stripe https://github.com/stripe/scoop-stripe-cli.git
scoop install stripe
```

**Option B (Manual):**
- Download from: https://github.com/stripe/stripe-cli/releases/latest
- Extract `stripe.exe` to a folder
- Add folder to Windows PATH
- Restart PowerShell

### ✅ Task 3: Login to Stripe CLI
```powershell
stripe login
```
- Browser will open
- Click "Allow access"
- Done!

### ✅ Task 4: Get Webhook Secret (Local)
1. Make sure backend is running: `mvn spring-boot:run`
2. Open NEW PowerShell window
3. Run: `stripe listen --forward-to http://localhost:8080/api/v1/webhooks/stripe`
4. **Copy the `whsec_...` secret** that appears
5. **Share it with me**

### ✅ Task 5: Get Webhook Secret (Sandbox)
1. In Stripe Dashboard → **Developers → Webhooks**
2. Click **"Add endpoint"**
3. URL: `https://your-sandbox-domain.com/api/v1/webhooks/stripe`
4. Select events: `payment_intent.succeeded`, `payment_intent.payment_failed`
5. Click **"Add endpoint"**
6. **Copy the `whsec_...` secret**
7. **Share it with me**

---

## 📤 SHARE WITH ME

Once you complete the tasks above, share:

1. ✅ Stripe Publishable Key: `pk_test_...`
2. ✅ Stripe Secret Key: `sk_test_...`
3. ✅ Local Webhook Secret: `whsec_...` (from `stripe listen`)
4. ✅ Sandbox Webhook Secret: `whsec_...` (from Dashboard)
5. ✅ Sandbox Domain: `https://...` (if you have one)

---

## 🔧 WHAT I'LL DO

After you share the keys, I'll:
- ✅ Update `application-dev.yml` with your keys
- ✅ Update `application-prod.yml` with your keys
- ✅ Configure webhook endpoints
- ✅ Test the integration
- ✅ Guide you through testing

---

## 🧪 TESTING (After Setup)

### Test Cards
- Success: `4242 4242 4242 4242`
- Decline: `4000 0000 0000 0002`
- 3D Secure: `4000 0025 0000 3155`

### Test Flow
1. Backend running: `mvn spring-boot:run`
2. Webhook listener: `stripe listen --forward-to http://localhost:8080/api/v1/webhooks/stripe`
3. Make test payment in frontend
4. Check logs for confirmation

---

**Ready? Start with Task 1!** 🚀

