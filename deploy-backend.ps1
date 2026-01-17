# TTelGo Backend Deployment Script
# Based on deploy-all.ps1 pattern

param(
    [string]$PemKeyPath = "D:\tiktel\ttelgo.pem",
    [string]$ServerUser = "ubuntu",
    [string]$ServerIP = "3.88.101.239",
    [switch]$SkipBuild = $false
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TTelGo Backend Deployment" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Server: $ServerIP" -ForegroundColor Yellow
Write-Host "User: $ServerUser" -ForegroundColor Yellow
Write-Host ""

# Validate PEM key
if (-not (Test-Path $PemKeyPath)) {
    Write-Host "ERROR: PEM key not found at: $PemKeyPath" -ForegroundColor Red
    exit 1
}

# Set SSH options as array
$sshOptionsArray = @("-i", $PemKeyPath, "-o", "StrictHostKeyChecking=no", "-o", "ConnectTimeout=10")

# Function to execute SSH command
function Invoke-SSHCommand {
    param([string]$Command)
    $result = & ssh.exe @sshOptionsArray "$ServerUser@$ServerIP" $Command 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "SSH Command failed: $Command" -ForegroundColor Red
        Write-Host $result -ForegroundColor Red
        throw "SSH command failed"
    }
    return $result
}

# Function to test SSH connection
function Test-SSHConnection {
    Write-Host "Testing SSH connection..." -ForegroundColor Yellow
    try {
        $result = Invoke-SSHCommand "echo 'Connection successful'"
        if ($result -match "Connection successful") {
            Write-Host "✓ SSH connection successful" -ForegroundColor Green
            return $true
        } else {
            Write-Host "✗ SSH connection failed" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "✗ SSH connection failed" -ForegroundColor Red
        return $false
    }
}

# Test connection
if (-not (Test-SSHConnection)) {
    Write-Host "Cannot connect to server. Please check:" -ForegroundColor Red
    Write-Host "  1. Server IP: $ServerIP" -ForegroundColor Yellow
    Write-Host "  2. PEM key path: $PemKeyPath" -ForegroundColor Yellow
    Write-Host "  3. Network connectivity" -ForegroundColor Yellow
    exit 1
}

# ==================== BACKEND DEPLOYMENT ====================
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "BACKEND DEPLOYMENT" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$backendPath = $PSScriptRoot
if (-not (Test-Path $backendPath)) {
    $backendPath = "D:\full-stack\ttelgo-backend"
}

if (-not (Test-Path $backendPath)) {
    Write-Host "ERROR: Backend path not found: $backendPath" -ForegroundColor Red
    exit 1
}

Set-Location $backendPath

# Build backend JAR
if (-not $SkipBuild) {
    Write-Host "Building backend JAR (this may take a few minutes)..." -ForegroundColor Yellow
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Backend build failed!" -ForegroundColor Red
        exit 1
    }
    Write-Host "✓ Backend build successful" -ForegroundColor Green
} else {
    Write-Host "Skipping backend build (using existing JAR)" -ForegroundColor Yellow
}

# Find the JAR file
$jarFile = Get-ChildItem -Path "target" -Filter "*.jar" -Exclude "*sources.jar","*javadoc.jar" | Select-Object -First 1

if (-not $jarFile) {
    Write-Host "ERROR: JAR file not found in target folder. Please build first." -ForegroundColor Red
    exit 1
}

Write-Host "Found JAR: $($jarFile.Name)" -ForegroundColor Cyan

# Upload backend JAR
Write-Host "Uploading backend JAR to server..." -ForegroundColor Yellow

try {
    # Create backend directory on server if it doesn't exist
    Invoke-SSHCommand "mkdir -p ~/ttelgo-backend"
    & scp.exe @sshOptionsArray "$($jarFile.FullName)" "${ServerUser}@${ServerIP}:~/ttelgo-backend/"
    Write-Host "✓ Backend JAR upload successful" -ForegroundColor Green
} catch {
    Write-Host "Backend upload failed!" -ForegroundColor Red
    exit 1
}

# Stop existing backend service
Write-Host "Stopping existing backend service..." -ForegroundColor Yellow
try {
    $stopCmd = 'sudo systemctl stop ttelgo-backend 2>/dev/null || pkill -f "ttelgo-backend.*jar" || true'
    Invoke-SSHCommand $stopCmd
    Start-Sleep -Seconds 3
    Write-Host "✓ Old service stopped" -ForegroundColor Green
} catch {
    Write-Host "Note: No existing service found (this is OK for first deployment)" -ForegroundColor Yellow
}

# Create systemd service file
Write-Host "Setting up backend service..." -ForegroundColor Yellow
$jarFileName = $jarFile.Name
$serviceFileContent = @"
[Unit]
Description=TTelGo Backend Service
After=network.target postgresql.service

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/ttelgo-backend
EnvironmentFile=/home/ubuntu/ttelgo-backend/.env
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod /home/ubuntu/ttelgo-backend/$jarFileName
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
"@

# Write service file to temp file and upload
$tempServiceFile = [System.IO.Path]::GetTempFileName()
Set-Content -Path $tempServiceFile -Value $serviceFileContent
$sshTarget = $ServerUser + '@' + $ServerIP
& scp.exe -i $PemKeyPath -o StrictHostKeyChecking=no $tempServiceFile "${sshTarget}:/tmp/ttelgo-backend.service"
Remove-Item $tempServiceFile
Invoke-SSHCommand "sudo mv /tmp/ttelgo-backend.service /etc/systemd/system/ttelgo-backend.service"
Invoke-SSHCommand "sudo systemctl daemon-reload"
Invoke-SSHCommand "sudo systemctl enable ttelgo-backend"

# Check if .env file exists on server
Write-Host "Checking for environment variables..." -ForegroundColor Yellow
$envCheckCmd = 'test -f ~/ttelgo-backend/.env && echo EXISTS || echo NOT_EXISTS'
$envExists = Invoke-SSHCommand $envCheckCmd

if ($envExists -match "NOT_EXISTS") {
    Write-Host "Creating .env file from existing configuration..." -ForegroundColor Yellow
    # Copy from old location if exists
    $createEnvCmd = @'
if [ -f /home/ubuntu/ttelgo-backend/.env ]; then
    echo "Using existing .env file"
elif [ -f /opt/ttelgo/.env ]; then
    cp /opt/ttelgo/.env /home/ubuntu/ttelgo-backend/.env
    echo "Copied .env from /opt/ttelgo"
else
    cat > /home/ubuntu/ttelgo-backend/.env << 'EOF'
DATABASE_URL=jdbc:postgresql://localhost:5432/ttelgo_db
DB_USERNAME=ttelgo_user
DB_PASSWORD=Post@gre_2026
ESIMGO_API_KEY=gSAXaGtFYQ3yKoda4A8kwksYBq1E4ZO14XmquhN_
SPRING_PROFILES_ACTIVE=prod
EOF
    echo "Created new .env file"
fi
'@
    Invoke-SSHCommand $createEnvCmd
    Write-Host "✓ Environment file configured" -ForegroundColor Green
} else {
    Write-Host "✓ Environment file exists" -ForegroundColor Green
}

# Start backend service
Write-Host "Starting backend service..." -ForegroundColor Yellow
try {
    Invoke-SSHCommand "sudo systemctl start ttelgo-backend"
    Start-Sleep -Seconds 5
    
    # Check service status
    $status = Invoke-SSHCommand "sudo systemctl status ttelgo-backend --no-pager | head -n 5"
    Write-Host $status
    
    Start-Sleep -Seconds 15
    
    if ($status -match "active \(running\)") {
        Write-Host "✓ Backend service started successfully!" -ForegroundColor Green
    } else {
        Write-Host "⚠ Checking service status..." -ForegroundColor Yellow
        $status2 = Invoke-SSHCommand "sudo systemctl is-active ttelgo-backend"
        if ($status2 -match "active") {
            Write-Host "✓ Backend service is active!" -ForegroundColor Green
        } else {
            Write-Host "⚠ Backend service may not be running properly" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "Backend service start failed!" -ForegroundColor Red
    $logCheckCmd = 'ssh -i "' + $PemKeyPath + '" ' + $ServerUser + '@' + $ServerIP + ' "sudo journalctl -u ttelgo-backend -n 50"'
    Write-Host "Check logs with: $logCheckCmd" -ForegroundColor Yellow
    exit 1
}

# Health check
Write-Host ""
Write-Host "Checking application health..." -ForegroundColor Yellow
Start-Sleep -Seconds 10
try {
    $healthCheck = Invoke-SSHCommand "curl -s http://localhost:8080/actuator/health"
    if ($healthCheck -match 'status.*UP') {
        Write-Host "✓ Application is healthy!" -ForegroundColor Green
        Write-Host $healthCheck -ForegroundColor Gray
    } else {
        Write-Host "⚠ Health check returned: $healthCheck" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠ Could not perform health check" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "BACKEND DEPLOYMENT COMPLETE!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Backend API: https://api.ttelgo.com" -ForegroundColor Cyan
Write-Host ""
Write-Host "To check backend logs:" -ForegroundColor Yellow
$logCmd = 'ssh -i "' + $PemKeyPath + '" ' + $ServerUser + '@' + $ServerIP + ' "sudo journalctl -u ttelgo-backend -f"'
Write-Host "  $logCmd" -ForegroundColor Gray
Write-Host ""
Write-Host "To check backend status:" -ForegroundColor Yellow
$statusCmd = 'ssh -i "' + $PemKeyPath + '" ' + $ServerUser + '@' + $ServerIP + ' "sudo systemctl status ttelgo-backend"'
Write-Host "  $statusCmd" -ForegroundColor Gray
Write-Host ""
Write-Host "To restart backend:" -ForegroundColor Yellow
$restartCmd = 'ssh -i "' + $PemKeyPath + '" ' + $ServerUser + '@' + $ServerIP + ' "sudo systemctl restart ttelgo-backend"'
Write-Host '  ' $restartCmd -ForegroundColor Gray
Write-Host ""

