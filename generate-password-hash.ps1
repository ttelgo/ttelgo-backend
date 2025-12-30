# PowerShell script to generate BCrypt hash for password
# This requires the backend to be running or Spring Security JAR to be available

Write-Host "Generating BCrypt hash for password: Admin@123456" -ForegroundColor Cyan
Write-Host ""

# Try to use Maven to run a simple Java class
$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendPath = Join-Path $scriptPath "ttelgo-backend"

Write-Host "To generate a BCrypt hash, you can:" -ForegroundColor Yellow
Write-Host "1. Use an online tool: https://bcrypt-generator.com/" -ForegroundColor Green
Write-Host "   - Enter password: Admin@123456" -ForegroundColor Green
Write-Host "   - Rounds: 10" -ForegroundColor Green
Write-Host "   - Copy the generated hash" -ForegroundColor Green
Write-Host ""
Write-Host "2. Or use this SQL to create the user (password will be hashed by the app):" -ForegroundColor Yellow
Write-Host "   First register via the frontend, then update role:" -ForegroundColor Green
Write-Host "   UPDATE users SET role = 'ADMIN' WHERE email = 'your-email@example.com';" -ForegroundColor Green
Write-Host ""
Write-Host "3. Or manually create user with a known BCrypt hash:" -ForegroundColor Yellow
Write-Host "   The hash in create-admin-user-script.sql should work." -ForegroundColor Green
Write-Host "   If it doesn't, generate a new one at https://bcrypt-generator.com/" -ForegroundColor Green

