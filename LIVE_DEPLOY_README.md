# Deploy to Live EC2 (Backend + Frontend + Stripe)

## Quick run (from your PC)

From the **ttelgo-backend** folder, run:

```powershell
.\deploy-full-live.ps1 -PemKeyPath "C:\Users\Zia\Desktop\TTelGO Website\ttelgo.pem" -ServerIP "34.195.92.171" -StripeSecretKey "sk_test_..." -StripePublishableKey "pk_test_..." -StripeWebhookSecret "whsec_..."
```

The script will:
1. **Backend**: SSH to EC2 → create `/home/ubuntu/ttelgo-backend/.env` with Stripe vars and `SPRING_PROFILES_ACTIVE=prod` → git pull → build → restart service if present.
2. **Frontend**: Pull from GitHub → build with live API URL and Stripe publishable key → deploy to `/var/www/ttelgo` → reload Nginx.

Stripe keys are **only** written to the server (in `.env`), never committed to git.

---

## If deploy fails: "No space left on device"

Your EC2 disk is full. On the server, free space first:

```bash
# SSH in
ssh -i "C:\Users\Zia\Desktop\TTelGO Website\ttelgo.pem" ubuntu@34.195.92.171

# Check usage
df -h
du -sh /home/ubuntu/* /var/www/* /tmp/* 2>/dev/null

# Remove old backups, logs, or Docker if unused
sudo rm -rf /var/www/ttelgo-backup-*   # old frontend backups
sudo journalctl --vacuum-time=3d       # shrink logs
```

Then run the deploy script again from your PC.

---

## Setting Stripe via AWS (optional)

If you prefer not to store Stripe in a file on the server:

1. **AWS Systems Manager Parameter Store** (recommended): Create parameters (SecureString) for `STRIPE_SECRET_KEY`, `STRIPE_PUBLISHABLE_KEY`, `STRIPE_WEBHOOK_SECRET`. On EC2, use an IAM role and have your app or a small script read them at startup.
2. **EC2 User Data / Launch config**: You can pass env vars there, but they’re less secure and visible in the console.
3. **Keep using the script**: It writes a `.env` only on the server (not in git). Ensure `.env` is in `.gitignore` (it is).

The deploy script uses the server `.env` so Stripe works without touching AWS console.

---

## Backend compile errors after merge

If the backend fails to build on the server with errors in `AdminEsimController`, `AdminOrderController`, `ApiKeyService`, `AuthService`, or `UserController`, those are from merged code that uses different types (e.g. `OrderStatus` enum in two places, or `OrderService.getOrdersByUserId`). The merge conflicts in `GlobalExceptionHandler`, `SecurityConfig`, `JwtAuthenticationFilter`, `OrderController`, `EsimController`, and `EsimService` are already resolved. Fix the remaining files to use the same enums and service APIs (e.g. `com.tiktel.ttelgo.common.domain.enums.*` and the existing `OrderService` / `EsimService` methods), then run the deploy script again.

---

## Backend not starting after deploy

If there is no systemd service yet, on the server:

```bash
cd /home/ubuntu/ttelgo-backend
set -a
source .env
set +a
java -jar target/ttelgo-backend-*.jar
```

To run in the background or create a systemd service, see `DEPLOYMENT.md`.
