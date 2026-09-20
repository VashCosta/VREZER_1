@echo off
title VREZER - Localhost Launcher
echo ========================================================
echo   VREZER - AI Career Intelligence Platform
echo ========================================================
echo.
echo Stopping any existing instances on port 9000...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :9000 ^| findstr LISTENING') do taskkill /f /pid %%a >nul 2>&1

echo Starting Spring Boot Full Stack Application on Localhost...
echo.
echo Web Application:     http://localhost:9000
echo API Version Status:  http://localhost:9000/api/analyzer/version
echo H2 Database Console: http://localhost:9000/h2-console
echo.
cd /d "%~dp0backend"
if exist target\analyzer-0.0.1-SNAPSHOT.jar (
    echo Launching pre-built production JAR...
    java -jar target\analyzer-0.0.1-SNAPSHOT.jar
) else (
    echo Starting via Maven Spring Boot...
    mvn spring-boot:run
)
pause
