# Laboratory Management System - Project Requirements

## Core Rules
1. **Strict Instructions**: Follow only the provided instructions. Do not add features or change things outside the scope of the request.
2. **Color Scheme**: Do NOT change the system's color scheme in any way.
3. **Clean Root**: The root directory must stay organized. Only the following folders are allowed:
   - `config`
   - `dist`
   - `jdk-17`
   - `patch`
   - `src`
   - `target`
   - `test`
   - `temp`
   - `tools`
   - `EXE`
   - `errors`
4. **No Extra Files**: Remove any temporary or scratch files after use. 
5. **Concise Responses**: Maintain extremely concise communication. Do not provide long explanations or redundant summaries. Focus on the core request and prioritize token efficiency.

## Build & Release Requirements
1. **Local Output**: All setup installers must be saved in `EXE\64bit` and `EXE\32bit`.
2. **External Backup**: Every build must be backed up to an external location (Default: `D:\Releases`).
3. **Versioned Subfolders**: Backups should be organized as `[BaseDir]\[Version]\64bit` and `[BaseDir]\[Version]\32bit`.
4. **Configuration**: The base release path is stored in `release_config.txt`. The build script should prompt for it if missing on the first run.

## Automation & Setup
1. **Environment Config**: Use `configure_env.bat` to set up dependencies (JDK 17, Inno Setup 6) on new machines.
2. **Build Process**: Use `build_setup.bat` for all installer generation.

---
*Always read this file before starting any new task.*
