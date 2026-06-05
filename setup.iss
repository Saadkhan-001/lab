#define AppName "Laboratory Management System"
#define AppVersion "1.1.0"

; --- DEMO vs NORMAL ---
#ifdef DEMO
  #define AppId "{{A1B2C3D4-E5F6-4A5B-9C8D-E7F8G9H0I1J2}"
  #define DefaultDirName "{autopf}\LaboratoryManagementSystem_DEMO"
  #define ModeSuffix "DEMO"
  #define SetupAppName AppName + " (DEMO)"
#else
  #define AppId "{{B2C3D4E5-F6A7-4B6C-AD9E-F8G9H0I1J2K3}"
  #define DefaultDirName "{autopf}\LaboratoryManagementSystem"
  #define ModeSuffix "NORMAL"
  #define SetupAppName AppName
#endif

; --- ARCHITECTURE ---
#ifdef BIT64
  #define ArchSuffix "x64"
#else
  #define ArchSuffix "x32"
#endif

#define OutputDir "EXE"
#define SetupFileName "LaboratoryManagementSystem_" + ModeSuffix + "_" + ArchSuffix + "_Setup"

[Setup]
AppId={#AppId}
AppName={#SetupAppName}
AppVersion={#AppVersion}
DefaultDirName={#DefaultDirName}
DefaultGroupName={#AppName}
OutputDir={#OutputDir}
OutputBaseFilename={#SetupFileName}
SetupIconFile=lab_icon.ico
; x64 uses ultra64 compression; x32 uses standard lzma to avoid OOM with bundled JRE
#ifdef BIT64
Compression=lzma2/ultra64
SolidCompression=yes
LZMAAlgorithm=1
LZMANumBlockThreads=2
#else
Compression=lzma2
SolidCompression=no
#endif
MinVersion=0,6.01

; --- ARCHITECTURE RESTRICTIONS ---
#ifdef BIT64
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64
#else
; x32 installer runs on both 32-bit and 64-bit Windows
ArchitecturesAllowed=x86 x64
#endif

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

; ===========================================================
; FILES SECTION
; x64: bundle the full dist (64-bit runtime + 64-bit exe)
; x32: bundle app + jre32 (32-bit JRE with JavaFX) + run32.bat
; ===========================================================
[Files]
#ifdef BIT64
Source: "dist\LaboratoryManagementSystem\LaboratoryManagementSystem.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "dist\LaboratoryManagementSystem\app\*"; DestDir: "{app}\app"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "dist\LaboratoryManagementSystem\runtime\*"; DestDir: "{app}\runtime"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "dist\LaboratoryManagementSystem\templates\*"; DestDir: "{app}\templates"; Flags: ignoreversion recursesubdirs createallsubdirs
#else
; x32: use run32.bat + bundled 32-bit JRE (NO 64-bit exe or runtime)
Source: "dist\LaboratoryManagementSystem\run32.bat"; DestDir: "{app}"; Flags: ignoreversion
Source: "dist\LaboratoryManagementSystem\launch.vbs"; DestDir: "{app}"; Flags: ignoreversion
Source: "dist\LaboratoryManagementSystem\app\*"; DestDir: "{app}\app"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "dist\LaboratoryManagementSystem\jre32\*"; DestDir: "{app}\jre32"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "dist\LaboratoryManagementSystem\templates\*"; DestDir: "{app}\templates"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "lab_icon.ico"; DestDir: "{app}"; Flags: ignoreversion
#endif
#ifdef DEMO
Source: "demo.txt"; DestDir: "{app}"; Flags: ignoreversion
#endif

; ===========================================================
; ICONS / SHORTCUTS
; x64: shortcut to the native .exe
; x32: shortcut to run32.bat with the lab icon
; ===========================================================
[Icons]
#ifdef BIT64
  #ifdef DEMO
Name: "{group}\{#SetupAppName}"; Filename: "{app}\LaboratoryManagementSystem.exe"; Parameters: "--demo-mode"
Name: "{commondesktop}\{#SetupAppName}"; Filename: "{app}\LaboratoryManagementSystem.exe"; Parameters: "--demo-mode"; Tasks: desktopicon
  #else
Name: "{group}\{#SetupAppName}"; Filename: "{app}\LaboratoryManagementSystem.exe"
Name: "{commondesktop}\{#SetupAppName}"; Filename: "{app}\LaboratoryManagementSystem.exe"; Tasks: desktopicon
  #endif
#else
  #ifdef DEMO
Name: "{group}\{#SetupAppName}"; Filename: "wscript.exe"; Parameters: """{app}\launch.vbs"" --demo-mode"; IconFilename: "{app}\lab_icon.ico"; WorkingDir: "{app}"
Name: "{commondesktop}\{#SetupAppName}"; Filename: "wscript.exe"; Parameters: """{app}\launch.vbs"" --demo-mode"; IconFilename: "{app}\lab_icon.ico"; WorkingDir: "{app}"; Tasks: desktopicon
  #else
Name: "{group}\{#SetupAppName}"; Filename: "wscript.exe"; Parameters: """{app}\launch.vbs"""; IconFilename: "{app}\lab_icon.ico"; WorkingDir: "{app}"
Name: "{commondesktop}\{#SetupAppName}"; Filename: "wscript.exe"; Parameters: """{app}\launch.vbs"""; IconFilename: "{app}\lab_icon.ico"; WorkingDir: "{app}"; Tasks: desktopicon
  #endif
#endif

; ===========================================================
; POST-INSTALL LAUNCH
; ===========================================================
[Run]
#ifdef BIT64
  #ifdef DEMO
Filename: "{app}\LaboratoryManagementSystem.exe"; Parameters: "--demo-mode"; Description: "{cm:LaunchProgram,{#SetupAppName}}"; Flags: nowait postinstall
  #else
Filename: "{app}\LaboratoryManagementSystem.exe"; Description: "{cm:LaunchProgram,{#SetupAppName}}"; Flags: nowait postinstall
  #endif
#else
  #ifdef DEMO
Filename: "wscript.exe"; Parameters: """{app}\launch.vbs"" --demo-mode"; WorkingDir: "{app}"; Description: "{cm:LaunchProgram,{#SetupAppName}}"; Flags: nowait postinstall runhidden
  #else
Filename: "wscript.exe"; Parameters: """{app}\launch.vbs"""; WorkingDir: "{app}"; Description: "{cm:LaunchProgram,{#SetupAppName}}"; Flags: nowait postinstall runhidden
  #endif
#endif
