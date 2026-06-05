@echo off
setlocal enabledelayedexpansion

:: =========================================================================
:: [CLINICAL MAINTENANCE] LABORATORY MANAGEMENT SYSTEM - MASTER SYNC
:: =========================================================================
:: This script resets the clinical test catalogue using the internal JRE.
:: No Python installation is required for this operation.
:: =========================================================================

title LMS - Clinical Protocol Synchronizer (Java Engine)
echo Preparing Master Sync Protocol...

:: 1. Detect Java in clinical workstation
set "JAVA_EXE=C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot\bin\java.exe"
if not exist "%JAVA_EXE%" (
    set "JAVA_EXE=java"
)

:: 2. Configure Clinical Classpath
set "CP=dist\LaboratoryManagementSystem\app\classes;target\LaboratoryManagementSystem-1.0-SNAPSHOT-shaded.jar"

:: 3. Execute Sync via Internal System Utility
echo Initializing Test Catalogue Sync Cycle...
"%JAVA_EXE%" -cp "%CP%" com.lab.lms.tools.SyncTool

if %ERRORLEVEL% NEQ 0 (
    echo [CRITICAL ERROR] Sync Tool failed with code %ERRORLEVEL%.
    echo Check for database locks or runtime environment integrity.
    echo Contact MSF Digital Solutions (+923165794442) for assistance.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo =========================================================================
echo CLINICAL REPOSITORY STATUS: SYNCHRONIZED
echo =========================================================================
echo Professional test repository synchronized from master seed.
echo Please restart the LMS application for changes to take effect.
echo =========================================================================
echo.
pause
