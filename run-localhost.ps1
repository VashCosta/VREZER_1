# VREZER - Localhost Launcher
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  VREZER — AI Career Intelligence Platform" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Checking port 9000..." -ForegroundColor Yellow
$conn = Get-NetTCPConnection -LocalPort 9000 -ErrorAction SilentlyContinue
if ($conn) {
    Write-Host "Stopping existing process ($($conn.OwningProcess)) on port 9000..." -ForegroundColor Yellow
    Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}

Write-Host "Starting Full Stack Server on Localhost..." -ForegroundColor Green
Write-Host "Web Application:      http://localhost:9000" -ForegroundColor Yellow
Write-Host "API Version Status:   http://localhost:9000/api/analyzer/version" -ForegroundColor Yellow
Write-Host "H2 Database Console:  http://localhost:9000/h2-console" -ForegroundColor Yellow
Write-Host ""

Set-Location -Path "$PSScriptRoot/backend"

if (Test-Path "target/analyzer-0.0.1-SNAPSHOT.jar") {
    Write-Host "Launching pre-built production JAR for instant startup..." -ForegroundColor Cyan
    java -jar target/analyzer-0.0.1-SNAPSHOT.jar
} else {
    Write-Host "Building and starting via Maven Spring Boot..." -ForegroundColor Cyan
    mvn spring-boot:run
}
