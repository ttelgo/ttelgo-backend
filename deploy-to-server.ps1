# TTelGo Backend Deployment Script
# This script deploys the latest backend to the production server

param(
    [Parameter(Mandatory=$true)]
    [string]$ServerIP,
    
    [Parameter(Mandatory=$true)]
    [string]$Username,
    
    [Parameter(Mandatory=$false)]
    [string]$DeployPath = "/opt/ttelgo",
    
    [Parameter(Mandatory=$false)]
    [string]$PemKeyPath = "D:\tiktel\ttelgo.pem"
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TTelGo Backend Deployment Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if PEM key exists
if (-not (Test-Path $PemKeyPath)) {
    Write-Host "ERROR: PEM key not found at: $PemKeyPath" -ForegroundColor Red
    exit 1
}

# Check if JAR file exists
$JarFile = "target\ttelgo-backend-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $JarFile)) {
    Write-Host "ERROR: JAR file not found. Please build the project first:" -ForegroundColor Red
    Write-Host "  mvn clean package -DskipTests" -ForegroundColor Yellow
    exit 1
}

Write-Host "✓ JAR file found: $JarFile" -ForegroundColor Green
Write-Host "✓ PEM key found: $PemKeyPath" -ForegroundColor Green
Write-Host ""

# Set PEM key permissions (required for SSH)
Write-Host "Setting PEM key permissions..." -ForegroundColor Yellow
icacls $PemKeyPath /inheritance:r | Out-Null
icacls $PemKeyPath /grant:r "$env:USERNAME`:R" | Out-Null

# Test SSH connection
Write-Host "Testing SSH connection to $Username@$ServerIP..." -ForegroundColor Yellow
try {
    $sshTest = ssh -i $PemKeyPath -o StrictHostKeyChecking=no -o ConnectTimeout=10 "$Username@$ServerIP" "echo Connection successful" 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "SSH connection failed"
    }
    Write-Host "✓ SSH connection successful" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Cannot connect to server. Please check:" -ForegroundColor Red
    Write-Host "  - Server IP: $ServerIP" -ForegroundColor Yellow
    Write-Host "  - Username: $Username" -ForegroundColor Yellow
    Write-Host "  - PEM key path: $PemKeyPath" -ForegroundColor Yellow
    Write-Host "  - Network connectivity" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# Create backup of old JAR on server
Write-Host "Creating backup of old backend on server..." -ForegroundColor Yellow
$backupCmd = "if [ -f $DeployPath/ttelgo-backend-0.0.1-SNAPSHOT.jar ]; then cp $DeployPath/ttelgo-backend-0.0.1-SNAPSHOT.jar $DeployPath/ttelgo-backend-0.0.1-SNAPSHOT.jar.backup.`$(date +%Y%m%d_%H%M%S); fi"
ssh -i $PemKeyPath "$Username@$ServerIP" $backupCmd 2>&1 | Out-Null
Write-Host "✓ Backup created" -ForegroundColor Green

# Stop the service
Write-Host "Stopping ttelgo service..." -ForegroundColor Yellow
$stopCmd = "sudo systemctl stop ttelgo 2>/dev/null || pkill -f ttelgo-backend || true"
ssh -i $PemKeyPath "$Username@$ServerIP" $stopCmd 2>&1 | Out-Null
Start-Sleep -Seconds 3
Write-Host "✓ Service stopped" -ForegroundColor Green

# Create deployment directory if it doesn't exist
Write-Host "Ensuring deployment directory exists..." -ForegroundColor Yellow
$mkdirCmd = "sudo mkdir -p $DeployPath; sudo chown ${Username}:${Username} $DeployPath"
ssh -i $PemKeyPath "$Username@$ServerIP" $mkdirCmd 2>&1 | Out-Null
Write-Host "✓ Directory ready" -ForegroundColor Green

# Upload new JAR file
Write-Host "Uploading new JAR file to server..." -ForegroundColor Yellow
scp -i $PemKeyPath -o StrictHostKeyChecking=no "$JarFile" "${Username}@${ServerIP}:${DeployPath}/ttelgo-backend-0.0.1-SNAPSHOT.jar"
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to upload JAR file" -ForegroundColor Red
    exit 1
}
Write-Host "✓ JAR file uploaded successfully" -ForegroundColor Green

# Set proper permissions
Write-Host "Setting file permissions..." -ForegroundColor Yellow
ssh -i $PemKeyPath "$Username@$ServerIP" "chmod 755 $DeployPath/ttelgo-backend-0.0.1-SNAPSHOT.jar" 2>&1 | Out-Null
Write-Host "✓ Permissions set" -ForegroundColor Green

# Start the service
Write-Host "Starting ttelgo service..." -ForegroundColor Yellow
$startCmd = "sudo systemctl start ttelgo 2>/dev/null || (cd $DeployPath; nohup java -jar -Dspring.profiles.active=prod ttelgo-backend-0.0.1-SNAPSHOT.jar > /dev/null 2>&1 &)"
ssh -i $PemKeyPath "$Username@$ServerIP" $startCmd 2>&1 | Out-Null
Start-Sleep -Seconds 5

# Check service status
Write-Host "Checking service status..." -ForegroundColor Yellow
$statusCheckCmd = "sudo systemctl status ttelgo --no-pager 2>/dev/null || echo Service running via nohup"
$status = ssh -i $PemKeyPath "$Username@$ServerIP" $statusCheckCmd 2>&1
Write-Host $status

# Wait a bit and check health
Write-Host "Waiting for application to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host "Checking application health..." -ForegroundColor Yellow
$healthCmd = "curl -s http://localhost:8080/actuator/health 2>/dev/null || echo Health check failed"
$healthCheck = ssh -i $PemKeyPath "$Username@$ServerIP" $healthCmd 2>&1
if ($healthCheck -match "status.*UP") {
    Write-Host "✓ Application is healthy!" -ForegroundColor Green
} else {
    Write-Host "⚠ Health check returned: $healthCheck" -ForegroundColor Yellow
    Write-Host "Please check logs manually:" -ForegroundColor Yellow
    Write-Host "  ssh -i $PemKeyPath ${Username}@${ServerIP} sudo journalctl -u ttelgo -n 50" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Deployment Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Server: $Username@$ServerIP" -ForegroundColor White
Write-Host "Deployment Path: $DeployPath" -ForegroundColor White
Write-Host ""
Write-Host "Useful commands:" -ForegroundColor Yellow
$logCmd = "ssh -i $PemKeyPath ${Username}@${ServerIP} sudo journalctl -u ttelgo -f"
$restartCmd = "ssh -i $PemKeyPath ${Username}@${ServerIP} sudo systemctl restart ttelgo"
$statusCmd = "ssh -i $PemKeyPath ${Username}@${ServerIP} sudo systemctl status ttelgo"
Write-Host "  Check logs: $logCmd" -ForegroundColor Cyan
Write-Host "  Restart: $restartCmd" -ForegroundColor Cyan
Write-Host "  Status: $statusCmd" -ForegroundColor Cyan
Write-Host ""
