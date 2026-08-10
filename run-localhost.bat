@echo off
title CareerForge AI - Localhost Launcher
echo ========================================================
echo   CareerForge AI - AI Career Intelligence Platform
echo ========================================================
echo.
echo Stopping any existing instances on port 9000...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :9000 ^| findstr LISTENING') do taskkill /f /pid %%a >nul 2>&1

echo Starting Spring Boot Full Stack Application on Localhost...
echo.
echo Web Application: http://localhost:9000
echo OpenAPI / Swagger UI: http://localhost:9000/swagger-ui.html
echo H2 Database Console: http://localhost:9000/h2-console
echo.
cd /d "%~dp0backend"
mvn clean spring-boot:run
pause
