# Email Configuration Script for TTelGo Backend
# Run this script to set email environment variables

Write-Host "=== TTelGo Email Configuration ===" -ForegroundColor Cyan
Write-Host ""

# Gmail SMTP Configuration
$env:MAIL_HOST = "smtp.gmail.com"
$env:MAIL_PORT = "587"
$env:MAIL_USERNAME = "support@ttelgo.com"

# IMPORTANT: Replace with your Gmail App Password
# Get it from: https://myaccount.google.com/apppasswords
Write-Host "Enter your Gmail App Password (16 characters):" -ForegroundColor Yellow
Write-Host "(Get it from: https://myaccount.google.com/apppasswords)" -ForegroundColor Gray
$appPassword = Read-Host -AsSecureString
$env:MAIL_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($appPassword)
)

$env:MAIL_FROM = "support@ttelgo.com"

Write-Host ""
Write-Host "Email configuration set!" -ForegroundColor Green
Write-Host ""
Write-Host "Configuration:" -ForegroundColor Cyan
Write-Host "  Host: $env:MAIL_HOST"
Write-Host "  Port: $env:MAIL_PORT"
Write-Host "  Username: $env:MAIL_USERNAME"
Write-Host "  Password: [HIDDEN]"
Write-Host "  From: $env:MAIL_FROM"
Write-Host ""
Write-Host "Now restart your Spring Boot application for changes to take effect." -ForegroundColor Yellow
Write-Host ""

