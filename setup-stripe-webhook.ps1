# Stripe Webhook Local Setup Script
# This script helps set up Stripe CLI and forward webhooks to local backend

Write-Host "Setting up Stripe Webhook locally..." -ForegroundColor Green

# Check if Stripe CLI is installed
$stripeInstalled = Get-Command stripe -ErrorAction SilentlyContinue

if (-not $stripeInstalled) {
    Write-Host "Stripe CLI not found. Please install it first:" -ForegroundColor Yellow
    Write-Host "1. Download from: https://github.com/stripe/stripe-cli/releases/latest" -ForegroundColor Cyan
    Write-Host "2. Extract stripe.exe to a folder in your PATH" -ForegroundColor Cyan
    Write-Host "3. Or use: scoop install stripe" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "After installation, run this script again." -ForegroundColor Yellow
    exit 1
}

Write-Host "Stripe CLI found!" -ForegroundColor Green
Write-Host ""

# Check if user is logged in
Write-Host "Checking Stripe login status..." -ForegroundColor Cyan
$loginCheck = stripe config --list 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "Please login to Stripe first:" -ForegroundColor Yellow
    Write-Host "Run: stripe login" -ForegroundColor Cyan
    Write-Host "This will open your browser to authorize." -ForegroundColor Cyan
    exit 1
}

Write-Host "Starting webhook listener..." -ForegroundColor Green
Write-Host "Webhook endpoint: http://localhost:8080/api/webhooks/stripe" -ForegroundColor Cyan
Write-Host ""
Write-Host "IMPORTANT: Copy the webhook signing secret (whsec_...) when it appears!" -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop the webhook listener" -ForegroundColor Yellow
Write-Host ""

# Start webhook forwarding
stripe listen --forward-to http://localhost:8080/api/webhooks/stripe

