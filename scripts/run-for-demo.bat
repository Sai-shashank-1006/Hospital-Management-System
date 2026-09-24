@echo off
REM MediFlow Demo Launcher
REM One-click startup: Docker + Tomcat + app + browser
REM For presenting to your teacher without touching a terminal

setlocal enabledelayedexpansion

cd /d "%~dp0.."
title MediFlow Demo Launcher

echo.
echo ===============================================
echo  MediFlow Hospital Management System
echo  Demo Launcher
echo ===============================================
echo.

REM --- 1. Start Docker Desktop if not running ---
echo [1/5] Starting Docker Desktop...
tasklist /FI "IMAGENAME eq Docker Desktop.exe" 2>NUL | find /I /N "Docker Desktop.exe">NUL
if "%ERRORLEVEL%"=="1" (
  start "" "C:\Users\saish\AppData\Local\Programs\DockerDesktop\Docker Desktop.exe"
  echo.  Waiting for Docker engine to be ready...
  timeout /t 8 /nobreak >NUL
)

REM --- 2. Wait for docker to be usable ---
echo.
echo [2/5] Checking Docker engine...
set docker_ok=0
for /L %%i in (1,1,30) do (
  docker info >NUL 2>&1
  if !ERRORLEVEL! equ 0 (
    set docker_ok=1
    echo.  Docker ready
    goto docker_ready
  )
  echo.  Attempt %%i...
  timeout /t 4 /nobreak >NUL
)
:docker_ready

REM --- 3. Start the containers ---
echo.
echo [3/5] Starting MySQL and App containers...
docker compose up -d >NUL 2>&1
if !ERRORLEVEL! neq 0 (
  echo.  ERROR: docker compose failed
  pause
  exit /b 1
)

REM Wait for app health check
echo.  Waiting for the app to be ready (this takes ~15 seconds)...
set app_ok=0
for /L %%i in (1,1,45) do (
  for /f %%A in ('curl -s --max-time 8 http://localhost:8090/hms/health 2^>NUL') do (
    echo %%A | find /I "UP" >NUL
    if !ERRORLEVEL! equ 0 (
      set app_ok=1
      echo.  App is ready
      goto app_ready
    )
  )
  timeout /t 4 /nobreak >NUL
)
:app_ready

if !app_ok! equ 0 (
  echo.  WARNING: App did not respond. It may take longer on first start.
)

REM --- 4. Start Tomcat ---
echo.
echo [4/5] Starting Apache Tomcat (the Jenkins-deployed copy)...
netstat -ano | find "9090" >NUL 2>&1
if !ERRORLEVEL! neq 0 (
  start "" minimized "C:\tools\apache-tomcat-10.1.60\bin\startup.bat"
  echo.  Waiting for Tomcat...
  timeout /t 8 /nobreak >NUL
)

REM --- 5. Open browser ---
echo.
echo [5/5] Opening the app in your browser...
timeout /t 2 /nobreak >NUL
start http://localhost:8090/hms/

echo.
echo ===============================================
echo.
echo ✓ MediFlow is running
echo.
echo   Docker app (for you):      http://localhost:8090/hms/
echo   Jenkins deployed (demo):   http://localhost:9090/hms/
echo   Jenkins CI/CD:             http://localhost:8080/job/Hospital-Management-System/
echo.
echo Demo credentials:
echo   Username: admin
echo   Password: admin123
echo.
echo The Tomcat console window (if visible) must stay open.
echo To stop everything: docker compose down
echo.
pause
