@echo off
setlocal

:: [CLINICAL MAINTENANCE] LABORATORY MANAGEMENT SYSTEM - CATALOGUE RESET
:: Native Windows Professional Tool (v7.0.6)
:: -------------------------------------------------------------------------
:: This tool standardizes your clinical test repository by wiping redundant
:: data. The LMS application will automatically re-install the correct
:: clinical protocols from 'tests_seed.json' on the next restart.
:: -------------------------------------------------------------------------
:: NO PYTHON IS REQUIRED. USES NATIVE INFRASTRUCTURE.
:: -------------------------------------------------------------------------

echo ====================================================
echo  [INFRASTRUCTURE] CLINICAL REPOSITORY INITIALIZATION
echo ====================================================
echo WARNING: This will permanently wipe existing clinical protocols.
echo Patient details and medical records will be preserved.
echo.

:: 1. Infrastructure Discovery
set "DB_PATH=%USERPROFILE%\.lablms\laboratory.db"
set "JAR_PATH=target\LaboratoryManagementSystem-1.0-SNAPSHOT-shaded.jar"
if not exist "%JAR_PATH%" (
    set "JAR_PATH=dist\LaboratoryManagementSystem\app\LaboratoryManagementSystem-1.0-SNAPSHOT-shaded.jar"
)

if not exist "%JAR_PATH%" (
    echo [ERROR] Clinical runtime infrastructure (JAR) not found.
    echo Please ensure you are running this from the project root.
    pause
    exit /b 1
)

:: 2. Execute Clinical Reset via Native Powershell + Internal SQLite Engine
powershell -NoProfile -Command "Write-Host 'Loading clinical engine...'; $jar = Join-Path (Get-Location) '%JAR_PATH%'; if (-not (Test-Path $jar)) { Write-Error 'JAR missing.'; exit 1 }; [Reflection.Assembly]::LoadFrom($jar) | Out-Null; $db = [System.IO.Path]::Combine($env:USERPROFILE, '.lablms', 'laboratory.db'); if (-not (Test-Path $db)) { Write-Error 'Database missing.'; exit 1 }; Write-Host 'Executing Master Wipe Protocol...'; $conn = [org.sqlite.JDBC]::Connect('jdbc:sqlite:' + $db, $null); $stmt = $conn.createStatement(); $stmt.execute('DELETE FROM test_parameters'); $stmt.execute('DELETE FROM tests'); $stmt.execute('DELETE FROM sqlite_sequence WHERE name IN (''tests'', ''test_parameters'')'); $conn.close(); Write-Host '[SUCCESS] Clinical catalogue initialized.' -ForegroundColor Green"

if %ERRORLEVEL% NEQ 0 (
    echo [CRITICAL ERROR] Synchronization failure. Ensure LMS is closed.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ====================================================
echo [STATUS] REPOSITORY INITIALIZED SUCCESSFULLY
echo ====================================================
echo Please start the Laboratory Management System to complete the sync.
echo ====================================================
pause
