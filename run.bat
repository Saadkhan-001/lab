@echo off
set "BASE_DIR=%~dp0"
set "JAVA_HOME=%BASE_DIR%jdk-17"
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
set "APP_DIR=%BASE_DIR%dist\LaboratoryManagementSystem\app"

:: Comprehensive classpath including critical JavaFX modules for Web/HTMLEditor
set "CP=%APP_DIR%\classes;%APP_DIR%\LaboratoryManagementSystem-1.0-SNAPSHOT-shaded.jar;%APP_DIR%\javafx-web-17.0.2-win.jar;%APP_DIR%\javafx-media-17.0.2-win.jar"

if not exist "%JAVA_EXE%" (
    echo [ERROR] Bundled JRE not found at: %JAVA_HOME%
    pause
    exit /b 1
)

echo Launching Clinical Laboratory Management System...

:: Kill any orphaned LMS Java processes from previous runs to prevent port/lock conflicts
for /f "tokens=2" %%a in ('tasklist /fi "imagename eq java.exe" /fo list 2^>nul ^| findstr /i "PID:"') do (
    wmic process where "ProcessId=%%a" get CommandLine 2>nul | findstr /i "com.lab.lms.Launcher" >nul 2>&1
    if not errorlevel 1 (
        echo [CLEANUP] Terminating orphaned LMS process: %%a
        taskkill /f /pid %%a >nul 2>&1
    )
)
:: Brief pause to let OS release ports and file handles
timeout /t 2 /nobreak >nul 2>&1

if exist errors\run_errors.txt del /q errors\run_errors.txt

:: 32-bit vs 64-bit Core Logic
:: Execute with 512MB RAM limit and SerialGC for stability on low-resource machines
set "VM_ARGS=-Xms128m -Xmx1024m -Dprism.order=sw -XX:ErrorFile=errors/hs_err_pid%%p.log"

:: Try Bundled JRE first
"%JAVA_EXE%" -version >nul 2>&1
if errorlevel 1 (
    echo [WARNING] Bundled JRE is incompatible or missing.
    echo [STATUS] Searching for system JRE/JDK installation...
    
    where java >nul 2>&1
    if errorlevel 0 (
        set "JAVA_EXE=java"
        echo [INFO] System Java detected. Attempting launch...
    ) else (
        echo [CRITICAL ERROR] No compatible Java installation found.
        echo Please install a 32-bit JRE from https://adoptium.net/
        pause
        exit /b 1
    )
)

:: Execute with 512MB RAM limit (Safe for 32-bit and low-RAM devices)
"%JAVA_EXE%" %VM_ARGS% "-Djava.library.path=%APP_DIR%" -cp "%CP%" com.lab.lms.Launcher %* > errors\run_errors.txt 2>&1

echo.
echo [STATUS] Application has exited.
if %ERRORLEVEL% NEQ 0 (
    echo [CRITICAL] Application exited with error code %ERRORLEVEL%
    echo Possible causes: Memory pressure, lock files, or driver conflict.
    echo.
    if exist errors\run_errors.txt (
        echo --- ERROR LOG START ---
        type errors\run_errors.txt
        echo --- ERROR LOG END ---
    )
)
echo Press any key to close this window.
pause
