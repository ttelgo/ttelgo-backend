# Script to stop all Java processes (useful when port conflicts occur)
Write-Host "Stopping all Java processes..."
Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Get-Process -Name javaw -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2
Write-Host "All Java processes stopped."
Write-Host "Checking ports 8080-8090..."
$ports = 8080..8090
foreach ($port in $ports) {
    $conn = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($conn) {
        Write-Host "Port $port is in use by PID $($conn.OwningProcess)"
    } else {
        Write-Host "Port $port is available"
    }
}

