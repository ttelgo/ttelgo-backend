# 🔑 How to Get Sandbox Webhook Secret - Step by Step

## ✅ Local Webhook Secret - DONE!

Your local webhook secret has been configured:
- ✅ Added to `application-dev.yml`
- ✅ Secret: `whsec_914baea8785a68fbbb148c821389c2e0c0d44d752b97fb151fd92c84cb42f7d7`

---

## 📋 Step 5: Get Sandbox Webhook Secret

### Step 1: Go to Stripe Dashboard

1. Open your browser
2. Go to: https://dashboard.stripe.com
3. Make sure you're in **Test Mode** (toggle in top right should say "Test mode")

### Step 2: Navigate to Webhooks

1. In the left sidebar, click **"Developers"**
2. Click **"Webhooks"** (under Developers)
3. You'll see a list of webhook endpoints (might be empty if this is your first time)

### Step 3: Add Webhook Endpoint

1. Click the **"+ Add endpoint"** button (top right)
2. You'll see a form to create a new webhook endpoint

### Step 4: Configure the Endpoint

Fill in the form:

**Endpoint URL:**
```
https://your-sandbox-domain.com/api/v1/webhooks/stripe
```

**OR if you don't have a sandbox domain yet, use:**
```
https://api.ttelgo.com/api/v1/webhooks/stripe
```

**OR if you're testing on a staging server:**
```
https://staging.ttelgo.com/api/v1/webhooks/stripe
```

**Description (optional):**
```
TTelGo Production/Sandbox Webhook
```

### Step 5: Select Events to Listen To

Click **"Select events"** and check these events:
- ✅ `payment_intent.succeeded`
- ✅ `payment_intent.payment_failed`
- ✅ `payment_intent.canceled` (optional but recommended)

Then click **"Add events"**

### Step 6: Create the Endpoint

1. Click **"Add endpoint"** button at the bottom
2. The webhook endpoint will be created

### Step 7: Get the Webhook Secret

1. After creating the endpoint, you'll see the endpoint details page
2. Look for **"Signing secret"** section
3. Click **"Reveal"** or **"Click to reveal"** next to the signing secret
4. You'll see: `whsec_xxxxxxxxxxxxx`
5. **Copy the entire `whsec_...` string**

---

## 📤 Share the Sandbox Webhook Secret

Once you have the sandbox webhook secret, share it with me and I'll:
1. ✅ Add it to `application-prod.yml` for sandbox/production
2. ✅ Verify the configuration
3. ✅ Help you test the integration

---

## 🎯 Quick Summary

1. ✅ Go to: https://dashboard.stripe.com → Developers → Webhooks
2. ✅ Click "+ Add endpoint"
3. ✅ Enter your webhook URL: `https://your-domain.com/api/v1/webhooks/stripe`
4. ✅ Select events: `payment_intent.succeeded`, `payment_intent.payment_failed`
5. ✅ Click "Add endpoint"
6. ✅ Click "Reveal" on the signing secret
7. ✅ Copy the `whsec_...` secret
8. ✅ Share it with me

---

## ❓ What's Your Sandbox Domain?

Before creating the webhook endpoint, I need to know:
- **What's your sandbox/staging domain?** (e.g., `https://api.ttelgo.com` or `https://staging.ttelgo.com`)
- **OR are you using the production domain for testing?**

Once you tell me the domain, I can give you the exact webhook URL to use!

---

**Ready? Start with Step 1!** 🚀

