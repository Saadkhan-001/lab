@echo off
echo [1/2] Extracting JDK...
if exist "tmp\jdk_extract" rd /s /q "tmp\jdk_extract"
mkdir tmp\jdk_extract
powershell -Command "Expand-Archive -Path 'tmp\jdk17.zip' -DestinationPath 'tmp\jdk_extract' -Force"
echo [2/2] Moving to jdk-17...
for /D %%i in ("tmp\jdk_extract\*") do (
    if exist "jdk-17" rd /s /q "jdk-17"
    move "%%i" "jdk-17"
)
rd /s /q "tmp\jdk_extract"
del /q "tmp\jdk17.zip"
echo [DONE] JDK extracted.
jdk-17\bin\java.exe -version
