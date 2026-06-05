@echo off
setlocal enabledelayedexpansion

echo ============================================================
echo   LABORATORY MANAGEMENT SYSTEM - ENVIRONMENT SETUP
echo ============================================================
echo.

set "MISSING_DEPS=0"

:: 1. Check Inno Setup 6
set "ISCC=C:\Program Files (x86)\Inno Setup 6\ISCC.exe"
if exist "%ISCC%" (
    echo [OK] Inno Setup 6 found.
) else (
    echo [MISSING] Inno Setup 6 NOT found at expected location.
    echo           Please download and install Inno Setup 6.
    echo           Link: https://jrsoftware.org/isdl.php
    set /a MISSING_DEPS+=1
)

:: 2. Check JDK 17 Folder
if exist "jdk-17" (
    echo [OK] Project JDK 17 folder found.
) else (
    echo [MISSING] Project JDK 17 folder NOT found in root.
    echo           Note: This project bundles JDK 17 for the installer.
    echo           Please download JDK 17 and place its contents in a 'jdk-17' folder.
    echo           Link: https://learn.microsoft.com/en-us/java/openjdk/download
    set /a MISSING_DEPS+=1
)

:: 3. Check Maven (optional but helpful)
mvn -version >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Maven detected.
) else (
    echo [INFO] Maven NOT detected in PATH. (Only needed for source compilation)
)

:: 4. Check Folder Structure & Cleanup
echo.
echo Verifying Project Structure...
set "REQUIRED_FOLDERS=config dist patch src target test temp tools EXE errors"
for %%F in (%REQUIRED_FOLDERS%) do (
    if exist "%%F" (
        echo   [OK] Folder '%%F' exists.
    ) else (
        echo   [FIX] Folder '%%F' was missing. Creating it...
        mkdir "%%F"
    )
)

:: 5. Check Release Config
echo.
if exist "release_config.txt" (
    for /f "usebackq tokens=*" %%a in ("release_config.txt") do set "RELEASE_BASE=%%a"
    echo [OK] Release configuration found.
    echo      Base path: !RELEASE_BASE!
) else (
    echo [INFO] Release configuration NOT found.
    echo        Creating default configuration...
    echo D:\Releases>"release_config.txt"
    echo [OK] Created release_config.txt with default path D:\Releases.
)

echo.
if !MISSING_DEPS! equ 0 (
    echo ============================================================
    echo   SUCCESS: Your environment is fully configured!
    echo   You can now run build_setup.bat to generate installers.
    echo ============================================================
) else (
    echo ============================================================
    echo   WAIT: There are missing dependencies. 
    echo   Please install them using the links provided above.
    echo ============================================================
)

echo.
pause
