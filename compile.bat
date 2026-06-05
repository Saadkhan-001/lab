@echo off
set "BASE_DIR=%~dp0"
set "JDK_DIR=%BASE_DIR%jdk-17"
set "JDK_URL=https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.11%%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.11_9.zip"

:: 1. Environment Verification & Auto-Setup
if exist "%JDK_DIR%\bin\javac.exe" goto :JDK_READY

echo [ENVIRONMENT] JDK 17 missing. Attempting Automated Provisioning...

if not exist "tmp" mkdir tmp
set "JDK_ZIP=tmp\jdk17_v2.zip"

echo [1/3] Downloading Portable JDK 17 (Adoptium) via cURL...
if exist "%JDK_ZIP%" del /q "%JDK_ZIP%"
curl.exe -L -o "%JDK_ZIP%" "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.11+9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.11_9.zip"

if not exist "%JDK_ZIP%" (
    echo [ERROR] Provisioning failed via cURL. Attempting PowerShell fallback...
    powershell -Command "$ErrorActionPreference = 'Stop'; $url = 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.11+9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.11_9.zip'; [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri $url -OutFile '%JDK_ZIP%'"
)

if not exist "%JDK_ZIP%" (
    echo [CRITICAL ERROR] JDK Provisioning failed. Please check internet connection.
    pause
    exit /b 1
)

echo [2/3] Extracting Infrastructure...
if exist "tmp\jdk_extract" rd /s /q "tmp\jdk_extract"
mkdir tmp\jdk_extract
powershell -Command "Expand-Archive -Path '%JDK_ZIP%' -DestinationPath 'tmp\jdk_extract' -Force"

echo [3/3] Finalizing local workspace...
for /D %%i in ("tmp\jdk_extract\*") do (
    if exist "%JDK_DIR%" rd /s /q "%JDK_DIR%"
    move "%%i" "%JDK_DIR%"
)
rd /s /q "tmp\jdk_extract"
del /q "%JDK_ZIP%"

echo [SUCCESS] JDK 17 (64-bit) Provisioned Successfully.
echo.

:JDK_READY
set "JAVA_HOME=%JDK_DIR%"
set "JAVAC=%JAVA_HOME%\bin\javac.exe"
set "M2_REPO=%USERPROFILE%\.m2\repository"
set "APP_DIR=dist\LaboratoryManagementSystem\app"
set "CP=%APP_DIR%\classes;%APP_DIR%\LaboratoryManagementSystem-1.0-SNAPSHOT-shaded.jar;%APP_DIR%\javafx-web-17.0.2-win.jar;%APP_DIR%\javafx-media-17.0.2-win.jar"
set "OUT=%APP_DIR%\classes"

echo Compiling all sources...
"%JAVAC%" -encoding UTF-8 -cp "%CP%" -d "%OUT%" src\main\java\com\lab\lms\models\*.java src\main\java\com\lab\lms\dao\*.java src\main\java\com\lab\lms\services\*.java src\main\java\com\lab\lms\controllers\*.java src\main\java\com\lab\lms\util\*.java src\main\java\com\lab\lms\*.java > errors\compile_errors.txt 2>&1
if %ERRORLEVEL% NEQ 0 (
    type errors\compile_errors.txt
    echo Compilation FAILED with error %ERRORLEVEL%
    exit /b %ERRORLEVEL%
)
if exist errors\compile_errors.txt del /q errors\compile_errors.txt
echo Compilation SUCCESSFUL.
echo Syncing resources...
xcopy /S /Y "src\main\resources\*" "%OUT%\" /Q

echo Injecting Clinical Runtime Extensions...
if exist "%M2_REPO%\org\openjfx\javafx-web\17.0.2\javafx-web-17.0.2-win.jar" (
    copy /Y "%M2_REPO%\org\openjfx\javafx-web\17.0.2\javafx-web-17.0.2-win.jar" "%APP_DIR%\"
) else (
    echo Warning: javafx-web jar not found in Maven repository.
)
if exist "%M2_REPO%\org\openjfx\javafx-media\17.0.2\javafx-media-17.0.2-win.jar" (
    copy /Y "%M2_REPO%\org\openjfx\javafx-media\17.0.2\javafx-media-17.0.2-win.jar" "%APP_DIR%\"
) else (
    echo Warning: javafx-media jar not found in Maven repository.
)

echo Build COMPLETE.
pause

