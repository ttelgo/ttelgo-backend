# Install Stripe CLI Script
Write-Host "Installing Stripe CLI..." -ForegroundColor Green

# Create installation directory
$installDir = "$env:USERPROFILE\stripe-cli"
$stripeExe = "$installDir\stripe.exe"

# Check if already installed
if (Test-Path $stripeExe) {
    Write-Host "Stripe CLI already installed at: $stripeExe" -ForegroundColor Yellow
    Write-Host "Checking version..." -ForegroundColor Cyan
    & $stripeExe --version
    exit 0
}

# Create directory
if (-not (Test-Path $installDir)) {
    New-Item -ItemType Directory -Path $installDir -Force | Out-Null
    Write-Host "Created directory: $installDir" -ForegroundColor Green
}

# Get latest release info
Write-Host "Fetching latest Stripe CLI release..." -ForegroundColor Cyan
try {
    $release = Invoke-RestMethod -Uri "https://api.github.com/repos/stripe/stripe-cli/releases/latest"
    $windowsAsset = $release.assets | Where-Object { $_.name -like "*windows*x86_64.zip" } | Select-Object -First 1
    
    if (-not $windowsAsset) {
        Write-Host "Error: Could not find Windows release" -ForegroundColor Red
        exit 1
    }
    
    $downloadUrl = $windowsAsset.browser_download_url
    $zipFile = "$installDir\stripe-cli.zip"
    
    Write-Host "Downloading from: $downloadUrl" -ForegroundColor Cyan
    Invoke-WebRequest -Uri $downloadUrl -OutFile $zipFile -UseBasicParsing
    
    Write-Host "Extracting..." -ForegroundColor Cyan
    Expand-Archive -Path $zipFile -DestinationPath $installDir -Force
    
    # Remove zip file
    Remove-Item $zipFile -Force
    
    Write-Host "Stripe CLI installed successfully!" -ForegroundColor Green
    Write-Host "Location: $stripeExe" -ForegroundColor Cyan
    
    # Check if it's in PATH
    $currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
    if ($currentPath -notlike "*$installDir*") {
        Write-Host ""
        Write-Host "Adding to PATH..." -ForegroundColor Yellow
        [Environment]::SetEnvironmentVariable("Path", "$currentPath;$installDir", "User")
        Write-Host "Added $installDir to PATH" -ForegroundColor Green
        Write-Host "Please restart PowerShell for PATH changes to take effect" -ForegroundColor Yellow
    }
    
    # Test installation
    Write-Host ""
    Write-Host "Testing installation..." -ForegroundColor Cyan
    & $stripeExe --version
    
    Write-Host ""
    Write-Host "Installation complete!" -ForegroundColor Green
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "1. Restart PowerShell (if PATH was updated)" -ForegroundColor Cyan
    Write-Host "2. Run: stripe login" -ForegroundColor Cyan
    Write-Host "3. Run: stripe listen --forward-to http://localhost:8080/api/webhooks/stripe" -ForegroundColor Cyan
    
} catch {
    Write-Host "Error installing Stripe CLI: $_" -ForegroundColor Red
    exit 1
}







