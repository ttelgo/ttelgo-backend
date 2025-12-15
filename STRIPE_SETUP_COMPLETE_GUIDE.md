# 🎯 Complete Stripe Integration Setup Guide

## Overview
This guide will help you set up Stripe integration for both **local development** and **sandbox/test environment**.

---

## 📋 STEP-BY-STEP CHECKLIST

### ✅ PART 1: Stripe Account Setup (YOU DO THIS)

#### Step 1.1: Create/Login to Stripe Account
1. Go to: https://dashboard.stripe.com/register
2. Create an account or login if you already have one
3. **Important**: Make sure you're in **Test Mode** (toggle in top right)

#### Step 1.2: Get Your API Keys
1. In Stripe Dashboard, go to: **Developers → API keys**
2. You'll see two keys:
   - **Publishable key**: `pk_test_...` (starts with `pk_test_`)
   - **Secret key**: `sk_test_...` (starts with `sk_test_`)
3. **Copy both keys** - you'll need them later

#### Step 1.3: Install Stripe CLI (For Local Webhook Testing)
**Choose ONE method:**

**Option A: Using Scoop (Easiest)**
```powershell
# Install Scoop first (if not installed)
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
irm get.scoop.sh | iex

# Install Stripe CLI
scoop bucket add stripe https://github.com/stripe/scoop-stripe-cli.git
scoop install stripe
```

**Option B: Manual Download**
1. Go to: https://github.com/stripe/stripe-cli/releases/latest
2. Download: `stripe_X.X.X_windows_x86_64.zip`
3. Extract `stripe.exe` to a folder (e.g., `C:\stripe-cli\`)
4. Add that folder to Windows PATH:
   - Press `Win + R`, type `sysdm.cpl`, press Enter
   - Go to "Advanced" tab → "Environment Variables"
   - Edit "Path" variable → Add the folder path
   - Restart PowerShell

**Option C: Using Chocolatey (if installed)**
```powershell
choco install stripe-cli
```

#### Step 1.4: Login to Stripe CLI
```powershell
stripe login
```
- This will open your browser
- Click "Allow access"
- You should see "Done! The Stripe CLI is configured"

---

### ✅ PART 2: Local Development Setup (I'LL HELP WITH CODE)

#### Step 2.1: Configure Environment Variables
**YOU NEED TO DO THIS:**
1. Create a file: `ttelgo-backend/.env` (if it doesn't exist)
2. Add these lines (replace with YOUR keys from Step 1.2):
```env
STRIPE_SECRET_KEY=sk_test_YOUR_SECRET_KEY_HERE
STRIPE_PUBLISHABLE_KEY=pk_test_YOUR_PUBLISHABLE_KEY_HERE
STRIPE_WEBHOOK_SECRET=whsec_YOUR_WEBHOOK_SECRET_HERE
```

**OR** I can update the `application-dev.yml` file directly with your keys.

#### Step 2.2: Start Backend Server
```powershell
cd ttelgo-backend
mvn spring-boot:run
```
Make sure it's running on `http://localhost:8080`

#### Step 2.3: Start Stripe Webhook Listener
**YOU DO THIS:**
Open a NEW PowerShell window and run:
```powershell
stripe listen --forward-to http://localhost:8080/api/webhooks/stripe
```

**IMPORTANT:** You'll see output like:
```
> Ready! Your webhook signing secret is whsec_xxxxxxxxxxxxx
```

**Copy this `whsec_...` secret!** 
- Add it to your `.env` file as `STRIPE_WEBHOOK_SECRET`
- Or tell me and I'll add it to the config

#### Step 2.4: Test Webhook Connection
**YOU DO THIS:**
In another PowerShell window:
```powershell
stripe trigger payment_intent.succeeded
```

You should see:
- Webhook received in the `stripe listen` window
- Logs in your backend console

---

### ✅ PART 3: Sandbox/Test Environment Setup

#### Step 3.1: Configure Sandbox Environment
**YOU NEED TO DO THIS:**
1. Get your Stripe test keys (same as Step 1.2)
2. In Stripe Dashboard → **Developers → Webhooks**
3. Click **"Add endpoint"**
4. Set endpoint URL: `https://your-sandbox-domain.com/api/webhooks/stripe`
5. Select events to listen to:
   - `payment_intent.succeeded`
   - `payment_intent.payment_failed`
6. Click **"Add endpoint"**
7. **Copy the webhook signing secret** (starts with `whsec_`)

#### Step 3.2: Update Production Config
**I'LL DO THIS** - Just provide me:
- Your Stripe Secret Key (Test mode)
- Your Stripe Publishable Key (Test mode)
- Your Webhook Secret from Step 3.1

---

## 🔧 WHAT I'LL CONFIGURE FOR YOU

Once you provide the keys, I'll:
1. ✅ Update `application-dev.yml` with your Stripe keys
2. ✅ Update `application-prod.yml` with your Stripe keys (for sandbox)
3. ✅ Verify webhook endpoint configuration
4. ✅ Test the integration

---

## 📝 INFORMATION I NEED FROM YOU

Please provide:

1. **Stripe Publishable Key (Test)**: `pk_test_...`
2. **Stripe Secret Key (Test)**: `sk_test_...`
3. **Webhook Secret (Local)**: `whsec_...` (from `stripe listen` command)
4. **Webhook Secret (Sandbox)**: `whsec_...` (from Stripe Dashboard)
5. **Sandbox Domain**: `https://your-sandbox-domain.com` (if you have one)

---

## 🧪 TESTING

### Test Cards (Use in Frontend)
- **Success**: `4242 4242 4242 4242`
- **Decline**: `4000 0000 0000 0002`
- **3D Secure**: `4000 0025 0000 3155`
- **Expiry**: Any future date (e.g., `12/25`)
- **CVC**: Any 3 digits (e.g., `123`)

### Test Payment Flow
1. Start backend: `mvn spring-boot:run`
2. Start webhook listener: `stripe listen --forward-to http://localhost:8080/api/webhooks/stripe`
3. Make a test payment in frontend
4. Check backend logs for payment confirmation
5. Check Stripe Dashboard → Payments for transaction

---

## 🚨 TROUBLESHOOTING

**"stripe: command not found"**
- Make sure Stripe CLI is installed and in PATH
- Restart PowerShell after adding to PATH

**"Authentication required"**
- Run `stripe login` first

**"Connection refused"**
- Make sure backend is running on port 8080
- Check: `http://localhost:8080/api/webhooks/stripe` is accessible

**"Invalid webhook signature"**
- Make sure webhook secret matches in config
- For local: Use secret from `stripe listen` output
- For sandbox: Use secret from Stripe Dashboard

---

## ✅ NEXT STEPS

1. **YOU DO**: Complete Part 1 (Stripe account setup)
2. **YOU DO**: Complete Part 2.1 (Get webhook secret from `stripe listen`)
3. **YOU SHARE**: Provide me the keys and secrets
4. **I DO**: Configure all the code and environment files
5. **WE TEST**: Together test the payment flow

---

## 📞 READY TO START?

1. First, complete **Step 1.1 - 1.4** (Stripe account & CLI setup)
2. Then share with me:
   - Your Stripe keys
   - Webhook secret from `stripe listen`
3. I'll configure everything else!

Let me know when you're ready! 🚀

