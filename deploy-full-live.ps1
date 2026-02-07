# TTelGo Full Live Deploy - Backend + Frontend + Stripe env vars on EC2
# Usage: .\deploy-full-live.ps1 -PemKeyPath "C:\...\ttelgo.pem" -ServerIP "34.195.92.171" -StripeSecretKey "sk_..." -StripePublishableKey "pk_..." -StripeWebhookSecret "whsec_..."

param(
    [Parameter(Mandatory=$true)]
    [string]$PemKeyPath,
    [string]$ServerIP = "34.195.92.171",
    [string]$ServerUser = "ubuntu",
    [string]$BackendPath = "/home/ubuntu/ttelgo-backend",
    [string]$FrontendDeployPath = "/var/www/ttelgo",
    [string]$BackendBranch = "feature/database-config-and-scripts",
    [string]$FrontendBranch = "master",
    [string]$BackendRepo = "https://github.com/ttelgo/ttelgo-backend.git",
    [string]$FrontendRepo = "https://github.com/ttelgo/ttelgo-frontend.git",
    [string]$LiveApiUrl = "https://www.ttelgo.com/api/v1",
    [Parameter(Mandatory=$false)]
    [string]$StripeSecretKey = "",
    [Parameter(Mandatory=$false)]
    [string]$StripePublishableKey = "",
    [Parameter(Mandatory=$false)]
    [string]$StripeWebhookSecret = ""
)

if (-not (Test-Path $PemKeyPath)) {
    Write-Host "ERROR: PEM key not found at: $PemKeyPath" -ForegroundColor Red
    exit 1
}

# Stripe keys from params or env
if ([string]::IsNullOrEmpty($StripeSecretKey)) { $StripeSecretKey = $env:STRIPE_SECRET_KEY }
if ([string]::IsNullOrEmpty($StripePublishableKey)) { $StripePublishableKey = $env:STRIPE_PUBLISHABLE_KEY }
if ([string]::IsNullOrEmpty($StripeWebhookSecret)) { $StripeWebhookSecret = $env:STRIPE_WEBHOOK_SECRET }

if ([string]::IsNullOrEmpty($StripeSecretKey) -or [string]::IsNullOrEmpty($StripePublishableKey) -or [string]::IsNullOrEmpty($StripeWebhookSecret)) {
    Write-Host "ERROR: Stripe keys required. Pass -StripeSecretKey, -StripePublishableKey, -StripeWebhookSecret or set env vars." -ForegroundColor Red
    exit 1
}

Write-Host "`n=== TTelGo Full Live Deploy ===" -ForegroundColor Green
Write-Host "Server: $ServerUser@$ServerIP" -ForegroundColor Cyan
Write-Host "Backend path: $BackendPath | Frontend: $FrontendDeployPath" -ForegroundColor Cyan

icacls $PemKeyPath /inheritance:r 2>$null
icacls $PemKeyPath /grant:r "$env:USERNAME`:R" 2>$null

# Escape for bash
$StripeSecretKeyEsc = $StripeSecretKey -replace "'", "'\\''"
$StripePublishableKeyEsc = $StripePublishableKey -replace "'", "'\\''"
$StripeWebhookSecretEsc = $StripeWebhookSecret -replace "'", "'\\''"
$LiveApiUrlEsc = $LiveApiUrl -replace "'", "'\\''"

# Build remote script: use single $ for bash vars so PowerShell does not expand them
$remoteScript = @"
set -e
export STRIPE_SECRET_KEY='$StripeSecretKeyEsc'
export STRIPE_PUBLISHABLE_KEY='$StripePublishableKeyEsc'
export STRIPE_WEBHOOK_SECRET='$StripeWebhookSecretEsc'
export LIVE_API_URL='$LiveApiUrlEsc'
export BACKEND_PATH='$BackendPath'
export FRONTEND_DEPLOY_PATH='$FrontendDeployPath'
export BACKEND_BRANCH='$BackendBranch'
export FRONTEND_BRANCH='$FrontendBranch'
export BACKEND_REPO='$BackendRepo'
export FRONTEND_REPO='$FrontendRepo'

echo "=== 0. Safe disk cleanup (never touches app source, .env, or config) ==="
df -h / 2>/dev/null | tail -1
# Only remove: old frontend backups, logs, package cache. Never remove live app or .env.
if [ -d /var/www ]; then
  for d in /var/www/ttelgo-backup-*; do
    [ -d "`$d" ] && sudo rm -rf "`$d" && echo "Removed old backup: `$d"
  done
fi
rm -f /tmp/deploy*.sh 2>/dev/null || true
sudo journalctl --vacuum-size=30M 2>/dev/null || true
sudo apt-get clean 2>/dev/null || true

echo "=== 1. Backend: Stripe env + pull + build + restart ==="
mkdir -p "`$BACKEND_PATH"
cd "`$BACKEND_PATH"
if [ ! -d .git ]; then
  git init
  git remote add origin "`$BACKEND_REPO"
  git fetch origin "`$BACKEND_BRANCH"
  git checkout -B "`$BACKEND_BRANCH" origin/"`$BACKEND_BRANCH"
else
  git fetch origin
  git checkout "`$BACKEND_BRANCH" 2>/dev/null || true
  git pull origin "`$BACKEND_BRANCH"
fi

# Env file for backend (not in git)
cat > .env << 'ENVEOF'
STRIPE_SECRET_KEY=$StripeSecretKeyEsc
STRIPE_PUBLISHABLE_KEY=$StripePublishableKeyEsc
STRIPE_WEBHOOK_SECRET=$StripeWebhookSecretEsc
SPRING_PROFILES_ACTIVE=prod
ENVEOF
chmod 600 .env

# Build backend (use env when running later)
chmod +x ./mvnw 2>/dev/null || true
if [ -f ./mvnw ]; then
  ./mvnw clean package -DskipTests -q
elif command -v mvn &>/dev/null; then
  mvn clean package -DskipTests -q
else
  echo "WARN: No Maven found. Install Java 17+ and Maven, or run: java -jar target/*.jar with .env sourced."
fi

# Restart backend if systemd service exists
if systemctl is-active --quiet ttelgo-backend 2>/dev/null; then
  sudo systemctl restart ttelgo-backend
  echo "Backend service restarted."
elif systemctl is-active --quiet ttelgo 2>/dev/null; then
  sudo systemctl restart ttelgo
  echo "Backend service restarted."
else
  echo "No systemd service found. To run backend: set -a; source .env; set +a; java -jar target/ttelgo-backend-*.jar"
fi

echo ""
echo "=== 2. Frontend: pull + build (with Stripe + API URL) + deploy ==="
TEMP_DIR=`$(mktemp -d)
cd `$TEMP_DIR
if [ -d ttelgo-frontend ]; then
  cd ttelgo-frontend
  git fetch origin
  git checkout "`$FRONTEND_BRANCH" 2>/dev/null || true
  git pull origin "`$FRONTEND_BRANCH"
else
  git clone -b "`$FRONTEND_BRANCH" "`$FRONTEND_REPO" ttelgo-frontend
  cd ttelgo-frontend
fi

# Frontend .env for build (Stripe publishable + API URL)
echo "VITE_API_BASE_URL=`$LIVE_API_URL" > .env
echo "VITE_STRIPE_PUBLISHABLE_KEY=`$STRIPE_PUBLISHABLE_KEY" >> .env

npm install --silent 2>/dev/null || npm install
npm run build

sudo mkdir -p "`$FRONTEND_DEPLOY_PATH"
sudo cp -r dist/* "`$FRONTEND_DEPLOY_PATH"/
sudo chown -R www-data:www-data "`$FRONTEND_DEPLOY_PATH"
sudo chmod -R 755 "`$FRONTEND_DEPLOY_PATH"
sudo nginx -t 2>/dev/null && sudo systemctl reload nginx 2>/dev/null || true

cd /
rm -rf `$TEMP_DIR
echo ""
echo "=== Done. Frontend: `$FRONTEND_DEPLOY_PATH | Backend: `$BACKEND_PATH ==="
"@

# Fix newlines for SSH (LF only)
$remoteScript = $remoteScript -replace "`r`n", "`n" -replace "`r", "`n"
$tempFile = [System.IO.Path]::GetTempFileName() + ".sh"
[System.IO.File]::WriteAllText($tempFile, $remoteScript, [System.Text.Encoding]::UTF8)

Write-Host "`nRunning deploy on server (via SSH stdin)..." -ForegroundColor Yellow
# Pipe script via SSH so we don't rely on SCP write to /tmp
Get-Content $tempFile -Raw | ssh -i $PemKeyPath -o StrictHostKeyChecking=no "${ServerUser}@${ServerIP}" "bash -s"
$ok = $LASTEXITCODE -eq 0
Remove-Item $tempFile -ErrorAction SilentlyContinue

if ($ok) {
    Write-Host "`nDeploy finished. Site: https://www.ttelgo.com" -ForegroundColor Green
    Write-Host "Stripe env vars are set in $BackendPath/.env on the server." -ForegroundColor Cyan
} else {
    Write-Host "`nDeploy had errors. Check output above." -ForegroundColor Red
    exit 1
}
