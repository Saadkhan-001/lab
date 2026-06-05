@echo off
:: ============================================================
::  BUILD STEP: Download & Setup 32-bit JRE for x32 Installer
::  Uses BellSoft Liberica Full x86 JDK 17 (includes JavaFX)
::  Run this ONCE before building the x32 installers.
:: ============================================================

set "DIST_DIR=dist\LaboratoryManagementSystem"
set "JRE32_DIR=%DIST_DIR%\jre32"
set "DOWNLOAD_URL=https://download.bell-sw.com/java/17.0.11+12/bellsoft-jre17.0.11+12-windows-i586-full.zip"
set "ZIP_FILE=tmp\liberica-jre17-x86-full.zip"

echo ============================================================
echo  SETUP: 32-bit JRE Download
echo ============================================================
echo.

:: Check if jre32 already exists
if exist "%JRE32_DIR%\bin\javaw.exe" (
    echo [OK] 32-bit JRE already present at %JRE32_DIR%
    echo      Skipping download.
    goto :DONE
)

:: Create tmp directory
if not exist "tmp" mkdir tmp

:: Download using PowerShell
echo [1/3] Downloading BellSoft Liberica Full x86 JRE 17...
echo       URL: %DOWNLOAD_URL%
echo       This is ~55MB, please wait...
echo.
powershell -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%ZIP_FILE%' -UseBasicParsing }"

if not exist "%ZIP_FILE%" (
    echo.
    echo ERROR: Download failed. Check your internet connection.
    pause
    exit /b 1
)

echo [2/3] Extracting JRE...
:: Extract to tmp first
if not exist "tmp\jre32_extract" mkdir tmp\jre32_extract
powershell -Command "Expand-Archive -Path '%ZIP_FILE%' -DestinationPath 'tmp\jre32_extract' -Force"

:: Find the extracted folder (it will be named bellsoft-jre17.0.x-windows-i586-full or similar)
for /D %%i in ("tmp\jre32_extract\*") do set "EXTRACTED_FOLDER=%%i"

if "%EXTRACTED_FOLDER%"=="" (
    echo ERROR: Could not find extracted JRE folder.
    pause
    exit /b 1
)

echo [3/3] Moving JRE to %JRE32_DIR%...
if exist "%JRE32_DIR%" rmdir /S /Q "%JRE32_DIR%"
move "%EXTRACTED_FOLDER%" "%JRE32_DIR%"

:: Clean up
del /Q "%ZIP_FILE%"
rmdir /S /Q "tmp\jre32_extract" 2>nul

echo.
echo ============================================================
echo  SUCCESS: 32-bit JRE ready at %JRE32_DIR%
echo ============================================================
echo.

:DONE
:: Quick verification
if exist "%JRE32_DIR%\bin\javaw.exe" (
    echo Verifying JRE architecture...
    "%JRE32_DIR%\bin\java.exe" -version 2>&1
    echo.
    echo Ready to build x32 installers!
) else (
    echo WARNING: javaw.exe not found. JRE setup may have failed.
    echo Expected: %JRE32_DIR%\bin\javaw.exe
)
pause
