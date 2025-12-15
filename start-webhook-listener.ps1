# Start Stripe Webhook Listener
# This will show the webhook secret that you need to copy

Write-Host "Starting Stripe Webhook Listener..." -ForegroundColor Green
Write-Host "Make sure your backend is running on http://localhost:8080" -ForegroundColor Yellow
Write-Host ""
Write-Host "IMPORTANT: When you see 'Ready! Your webhook signing secret is whsec_...'" -ForegroundColor Cyan
Write-Host "Copy the entire whsec_... string and share it!" -ForegroundColor Cyan
Write-Host ""
Write-Host "Press Ctrl+C to stop the listener" -ForegroundColor Yellow
Write-Host ""

stripe listen --forward-to http://localhost:8080/api/webhooks/stripe

