# Stripe Webhook Local Setup Guide

## Quick Setup Steps

### Step 1: Install Stripe CLI

**Option A: Using Scoop (Recommended for Windows)**
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
4. Add that folder to your Windows PATH:
   - Search "Environment Variables" in Windows
   - Edit "Path" variable
   - Add the folder path
   - Restart PowerShell

**Option C: Using Chocolatey (if installed)**
```powershell
choco install stripe-cli
```

### Step 2: Login to Stripe

```powershell
stripe login
```

This will:
- Open your browser
- Ask you to authorize the CLI
- Link your Stripe account

### Step 3: Start Webhook Listener

Make sure your backend is running on `http://localhost:8080`, then run:

```powershell
stripe listen --forward-to http://localhost:8080/api/v1/webhooks/stripe
```

**IMPORTANT:** You'll see output like:
```
> Ready! Your webhook signing secret is whsec_xxxxxxxxxxxxx
```

**Copy this `whsec_...` secret!** You'll need it for configuration.

### Step 4: Test Webhook

In a new terminal, trigger a test event:
```powershell
stripe trigger payment_intent.succeeded
```

This will send a test webhook to your local backend.

## Using the Setup Script

After installing Stripe CLI, you can use the provided script:

```powershell
.\setup-stripe-webhook.ps1
```

## What You Need to Share

After setup, provide these from your Stripe Dashboard:

1. **Secret Key** (Test mode): `sk_test_...`
   - Location: Dashboard → Developers → API keys

2. **Publishable Key** (Test mode): `pk_test_...`
   - Location: Dashboard → Developers → API keys

3. **Webhook Secret**: `whsec_...`
   - From the `stripe listen` command output

## Troubleshooting

**"stripe: command not found"**
- Make sure Stripe CLI is installed and in your PATH
- Restart PowerShell after adding to PATH

**"Authentication required"**
- Run `stripe login` first

**"Connection refused"**
- Make sure your backend is running on port 8080
- Check: `http://localhost:8080/api/v1/webhooks/stripe` is accessible

## Next Steps

Once you have:
- ✅ Stripe CLI installed
- ✅ Logged in (`stripe login`)
- ✅ Webhook secret from `stripe listen`

Share these with me and I'll configure the backend integration!

