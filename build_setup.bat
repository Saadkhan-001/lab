@echo off
setlocal enabledelayedexpansion

set "ISCC=C:\Program Files (x86)\Inno Setup 6\ISCC.exe"

if not exist "%ISCC%" goto :NO_ISCC
goto :GET_VERSION

:NO_ISCC
echo ERROR: Inno Setup 6 not found at %ISCC%
pause
exit /b 1

:GET_VERSION
:: Extract version from setup.iss
for /f "tokens=3 delims= " %%I in ('findstr /C:"#define AppVersion" setup.iss') do (
    set "VERSION=%%~I"
)
set "VERSION=%VERSION:"=%"
set "VERSION=v%VERSION%"
echo Version detected: %VERSION%

:CHECK_RELEASE_PATH
:: Check for persistent release path configuration
set "RELEASE_BASE=D:\Releases"
if exist "release_config.txt" (
    for /f "usebackq tokens=*" %%a in ("release_config.txt") do set "RELEASE_BASE=%%a"
) else (
    echo !RELEASE_BASE!>"release_config.txt"
)
:: Remove any trailing spaces/newlines from the file read
for /f "tokens=* delims=" %%a in ("%RELEASE_BASE%") do set "RELEASE_BASE=%%a"

echo Release Base Directory: %RELEASE_BASE%
set "RELEASE_DIR=%RELEASE_BASE%\%VERSION%"
echo Target Release Directory: %RELEASE_DIR%

:CHECK_JRE32
:: Verify the 32-bit JRE is present before building x32 installers
if not exist "dist\LaboratoryManagementSystem\jre32\bin\javaw.exe" goto :NEED_JRE32
goto :START_BUILD

:NEED_JRE32
echo ============================================================
echo  MISSING: 32-bit JRE not found.
echo  Running setup_x32_jre.bat to download it first...
echo ============================================================
echo.
call setup_x32_jre.bat
if not exist "dist\LaboratoryManagementSystem\jre32\bin\javaw.exe" (
    echo ERROR: 32-bit JRE setup failed. Cannot build x32 installers.
    pause
    exit /b 1
)

:START_BUILD
:: Create local output directories if they don't exist
if not exist "EXE\64bit" mkdir "EXE\64bit"
if not exist "EXE\32bit" mkdir "EXE\32bit"
if not exist "patch" mkdir "patch"
if not exist "temp" mkdir "temp"

:: Create external release directories
if not exist "%RELEASE_DIR%\64bit" mkdir "%RELEASE_DIR%\64bit"
if not exist "%RELEASE_DIR%\32bit" mkdir "%RELEASE_DIR%\32bit"

echo.
echo ============================================================
echo   LABORATORY MANAGEMENT SYSTEM - MASTER BUILD PROTOCOL
echo ============================================================
echo.
echo [1] Build ALL (Standard + Demo, x64 + x32)
echo [2] Build NORMAL x32 ONLY
echo [3] Build NORMAL x64 ONLY
echo [4] Build ALL NORMAL (x64 + x32)
echo [5] Build ALL x32 (Normal + Demo)
echo.
set /p choice="Select Build Option [1-5]: "

:: STEP 1: CLEAN UP old installers
echo Cleaning old installers...
del /Q "LaboratoryManagementSystem_*_Setup.exe" 2>nul
del /Q "EXE\64bit\LaboratoryManagementSystem_*_Setup.exe" 2>nul
del /Q "EXE\32bit\LaboratoryManagementSystem_*_Setup.exe" 2>nul

if "%choice%"=="2" goto :BUILD_X32
if "%choice%"=="3" goto :BUILD_X64
if "%choice%"=="4" goto :BUILD_NORMAL
if "%choice%"=="5" goto :BUILD_ALL_X32

:BUILD_ALL
echo.
echo [1/6] Building: NORMAL x64...
"%ISCC%" /q /dBIT64 /O"EXE\64bit" setup.iss
echo [2/6] Building: NORMAL x32...
"%ISCC%" /q /O"EXE\32bit" setup.iss
echo [3/6] Building: DEMO x64...
"%ISCC%" /q /dBIT64 /dDEMO /O"EXE\64bit" setup.iss
echo [4/6] Building: DEMO x32...
"%ISCC%" /q /dDEMO /O"EXE\32bit" setup.iss
echo [5/6] Building: RECOVER x64...
"%ISCC%" /q /dBIT64 /dRECOVER /O"EXE\64bit" setup.iss
echo [6/6] Building: RECOVER x32...
"%ISCC%" /q /dRECOVER /O"EXE\32bit" setup.iss
goto :FINISH_BUILD

:BUILD_X32
echo Building: NORMAL x32 ONLY...
"%ISCC%" /q /O"EXE\32bit" setup.iss
goto :FINISH_BUILD

:BUILD_X64
echo Building: NORMAL x64 ONLY...
"%ISCC%" /q /dBIT64 /O"EXE\64bit" setup.iss
goto :FINISH_BUILD

:BUILD_NORMAL
echo Building: NORMAL x64...
"%ISCC%" /q /dBIT64 /O"EXE\64bit" setup.iss
echo Building: NORMAL x32...
"%ISCC%" /q /O"EXE\32bit" setup.iss
goto :FINISH_BUILD

:BUILD_ALL_X32
echo Building: NORMAL x32...
"%ISCC%" /q /O"EXE\32bit" setup.iss
echo Building: DEMO x32...
"%ISCC%" /q /dDEMO /O"EXE\32bit" setup.iss
goto :FINISH_BUILD

:FINISH_BUILD

:: STEP 3: COPY TO EXTERNAL RELEASE DIRECTORY
echo.
echo Copying installers to external release directory...
copy /Y "EXE\64bit\*" "%RELEASE_DIR%\64bit\" >nul 2>nul
copy /Y "EXE\32bit\*" "%RELEASE_DIR%\32bit\" >nul 2>nul

echo.
echo ============================================================
echo   BUILD COMPLETE - Generated Installers:
echo ============================================================
echo [Local EXE Check]
dir /B "EXE\64bit\LaboratoryManagementSystem_*_Setup.exe" 2>nul
dir /B "EXE\32bit\LaboratoryManagementSystem_*_Setup.exe" 2>nul
echo.
echo [External Release Check: %RELEASE_DIR%]
dir /B "%RELEASE_DIR%\64bit\LaboratoryManagementSystem_*_Setup.exe" 2>nul
dir /B "%RELEASE_DIR%\32bit\LaboratoryManagementSystem_*_Setup.exe" 2>nul
echo.
pause
