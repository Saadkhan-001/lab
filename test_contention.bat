@echo off
set "BASE_DIR=%~dp0"
set "JAVA_HOME=%BASE_DIR%jdk-17"
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
set "APP_DIR=%BASE_DIR%dist\LaboratoryManagementSystem\app"
set "CP=%APP_DIR%\classes;%APP_DIR%\LaboratoryManagementSystem-1.0-SNAPSHOT-shaded.jar;%APP_DIR%\javafx-web-17.0.2-win.jar;%APP_DIR%\javafx-media-17.0.2-win.jar"
set "VM_ARGS=-Xms128m -Xmx1024m -Dprism.order=sw"

echo Running Transactional Contention Test...
"%JAVA_EXE%" %VM_ARGS% "-Djava.library.path=%APP_DIR%" -cp "%CP%" com.lab.lms.TestPDFContention
pause
