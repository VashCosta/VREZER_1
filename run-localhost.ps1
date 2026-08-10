# CareerForge AI - Localhost Launcher
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  CareerForge AI - AI Career Intelligence Platform" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Stopping any existing processes on port 8000..." -ForegroundColor Yellow
$conn = Get-NetTCPConnection -LocalPort 8000 -ErrorAction SilentlyContinue
if ($conn) {
    Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
}

Write-Host "Starting Full Stack Server on Localhost..." -ForegroundColor Green
Write-Host "Web Application: http://localhost:8000" -ForegroundColor Yellow
Write-Host "OpenAPI / Swagger: http://localhost:8000/swagger-ui.html" -ForegroundColor Yellow
Write-Host "H2 Database Console: http://localhost:8000/h2-console" -ForegroundColor Yellow
Write-Host ""
Set-Location -Path "$PSScriptRoot/backend"
mvn clean spring-boot:run
