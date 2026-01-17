# Quick test script for live API
$tokenResponse = Invoke-RestMethod -Uri "https://ttelgo.com/api/v1/auth/test/token" -Method POST -ContentType "application/json" -Body '{"userId":1,"email":"test@ttelgo.com","role":"USER"}'
$token = $tokenResponse.accessToken
Write-Host "Token obtained: $($token.Substring(0,50))..."
Write-Host ""
Write-Host "Testing bundles endpoint:"
$bundles = Invoke-RestMethod -Uri "https://ttelgo.com/api/v1/bundles?size=5" -Headers @{"Authorization"="Bearer $token"}
Write-Host "Success! Found $($bundles.data.bundles.Count) bundles"
$bundles.data.bundles | Select-Object -First 3 | Format-Table name, description, price

