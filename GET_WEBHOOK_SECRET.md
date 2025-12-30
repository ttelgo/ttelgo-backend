# 🔑 How to Get Local Webhook Secret - Step by Step

## ✅ Your Stripe Keys Are Already Configured!

I've already added your keys to the configuration files:
- ✅ Publishable Key: `pk_test_51RISllRx29EprcbCkg0MG1jBTTSt9ujxD2ufXUrYNH8L2x7Pg8rY110jPFvVQzWTG0vFigV4Zc04SNZdGbhpCyKT00Vay8QQmY`
- ✅ Secret Key: `sk_test_51RISllRx29EprcbCQVsHp5z7yFlkSuiS09PqNxKwPJyYM4HE0JJZBU6qHf58QjZjnsFxHtqT6QJwAxCATvBYK0q100o54F0lt0`

---

## 📋 Step-by-Step: Get Local Webhook Secret

### Step 1: Check if Stripe CLI is Installed

Open PowerShell and run:
```powershell
stripe --version
```

**If you see a version number** → Go to Step 2
**If you see "command not found"** → Install Stripe CLI first (see below)

#### Install Stripe CLI (if needed):

**Option A - Using Scoop (Easiest):**
```powershell
# Install Scoop first (if not installed)
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
irm get.scoop.sh | iex

# Install Stripe CLI
scoop bucket add stripe https://github.com/stripe/scoop-stripe-cli.git
scoop install stripe
```

**Option B - Manual Download:**
1. Go to: https://github.com/stripe/stripe-cli/releases/latest
2. Download: `stripe_X.X.X_windows_x86_64.zip`
3. Extract `stripe.exe` to a folder (e.g., `C:\stripe-cli\`)
4. Add folder to Windows PATH:
   - Press `Win + R`, type `sysdm.cpl`, press Enter
   - Go to "Advanced" tab → "Environment Variables"
   - Edit "Path" variable → Add the folder path
   - Restart PowerShell

---

### Step 2: Login to Stripe CLI

```powershell
stripe login
```

**What happens:**
- Browser will open automatically
- Click "Allow access" in the browser
- You should see: "Done! The Stripe CLI is configured"

---

### Step 3: Start Your Backend Server

**Open PowerShell Window #1** and run:
```powershell
cd D:\tiktel\full-stack\ttelgo-backend
mvn spring-boot:run
```

**Wait until you see:**
```
Started TtelgoApplication in X.XXX seconds
```

**Keep this window open!** The backend must be running.

---

### Step 4: Start Stripe Webhook Listener

**Open a NEW PowerShell Window #2** (keep backend running in Window #1) and run:

```powershell
stripe listen --forward-to http://localhost:8080/api/v1/webhooks/stripe
```

**What you'll see:**
```
> Ready! Your webhook signing secret is whsec_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

**🎯 THIS IS YOUR LOCAL WEBHOOK SECRET!**

**Copy the entire `whsec_...` string** - it will look something like:
```
whsec_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456
```

**Keep this window open!** The webhook listener must keep running.

---

### Step 5: Test the Webhook Connection

**Open a THIRD PowerShell Window #3** and run:

```powershell
stripe trigger payment_intent.succeeded
```

**What should happen:**
- In Window #2 (webhook listener): You'll see webhook events being received
- In Window #1 (backend): You'll see logs showing the webhook was processed

**If you see webhook events in both windows → Success! ✅**

---

## 📤 Share the Webhook Secret

Once you have the `whsec_...` secret from Step 4, **share it with me** and I'll:
1. Add it to your `application-dev.yml` for local development
2. Help you set up the sandbox webhook secret next

---

## 🎯 Quick Summary

1. ✅ Install Stripe CLI (if not installed)
2. ✅ Run `stripe login`
3. ✅ Start backend: `mvn spring-boot:run` (Window #1)
4. ✅ Run `stripe listen --forward-to http://localhost:8080/api/v1/webhooks/stripe` (Window #2)
5. ✅ Copy the `whsec_...` secret that appears
6. ✅ Test with `stripe trigger payment_intent.succeeded` (Window #3)
7. ✅ Share the `whsec_...` secret with me

---

## 🚨 Troubleshooting

**"stripe: command not found"**
- Stripe CLI is not installed or not in PATH
- Install it using one of the methods above
- Restart PowerShell after installation

**"Authentication required"**
- Run `stripe login` first
- Make sure browser opens and you click "Allow access"

**"Connection refused" or "Failed to connect"**
- Make sure backend is running on port 8080
- Check: `http://localhost:8080/api/v1/webhooks/stripe` is accessible
- Try: `curl http://localhost:8080/api/v1/webhooks/stripe` (should return an error, but connection should work)

**No webhook secret appears**
- Make sure backend is running first
- Check that port 8080 is not blocked by firewall
- Try running `stripe listen` again

---

**Ready? Start with Step 1!** 🚀

Once you have the `whsec_...` secret, share it with me and we'll continue with the sandbox setup!

