@echo off
echo ============================================================
echo  LABORATORY SYSTEM REPAIR & RECOVERY PROTOCOL
echo ============================================================
echo.
echo [1] Killing all hung Java processes...
taskkill /F /IM java.exe /T >nul 2>&1
taskkill /F /IM javaw.exe /T >nul 2>&1
taskkill /F /IM LaboratoryManagementSystem.exe /T >nul 2>&1

echo [2] Clearing database locks and isolated runtime files...
set "DB_DIR=%USERPROFILE%\.lablms"
if exist "%DB_DIR%" (
    del /Q "%DB_DIR%\*.db-wal" >nul 2>&1
    del /Q "%DB_DIR%\*.db-shm" >nul 2>&1
    echo   - Database locks cleared.
)

echo [3] Releasing LIS Port 5000...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :5000') do (
    taskkill /F /PID %%a >nul 2>&1
)

echo.
echo [COMPLETE] System state has been reset.
echo.
echo ACTION: Now run "run32.bat" to start the stabilized 32-bit system.
echo.
pause
