@echo off
setlocal

REM Build script for Manager Control Panel (Windows)
echo ==========================================
echo   Building Manager Control Panel
echo ==========================================
echo.

REM Get script directory
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

REM Output directory
set "OUTPUT_DIR=..\..\Deploy\Manager"
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

echo Building for multiple platforms...
echo.

REM Build for Windows (64-bit)
echo [^>] Building for Windows (amd64)...
set GOOS=windows
set GOARCH=amd64
go build -o "%OUTPUT_DIR%\ManagerControlPanel.exe" main.go
echo   [OK] Created: ManagerControlPanel.exe

REM Build for Linux (64-bit)
echo [^>] Building for Linux (amd64)...
set GOOS=linux
set GOARCH=amd64
go build -o "%OUTPUT_DIR%\ManagerControlPanel" main.go
echo   [OK] Created: ManagerControlPanel (Linux)

REM Build for macOS (64-bit Intel)
echo [^>] Building for macOS (amd64)...
set GOOS=darwin
set GOARCH=amd64
go build -o "%OUTPUT_DIR%\ManagerControlPanel-mac" main.go
echo   [OK] Created: ManagerControlPanel-mac (Intel)

REM Build for macOS (ARM64 - Apple Silicon)
echo [^>] Building for macOS (arm64)...
set GOOS=darwin
set GOARCH=arm64
go build -o "%OUTPUT_DIR%\ManagerControlPanel-mac-arm64" main.go
echo   [OK] Created: ManagerControlPanel-mac-arm64 (Apple Silicon)

echo.
echo ==========================================
echo   Build Complete!
echo ==========================================
echo.
echo Output files created in: %OUTPUT_DIR%
echo.
echo Files:
echo   - ManagerControlPanel.exe        (Windows)
echo   - ManagerControlPanel            (Linux)
echo   - ManagerControlPanel-mac        (macOS Intel)
echo   - ManagerControlPanel-mac-arm64  (macOS ARM)
echo.
echo To run:
echo   Windows: ManagerControlPanel.exe
echo   Linux:   ./ManagerControlPanel
echo   macOS:   ./ManagerControlPanel-mac or ./ManagerControlPanel-mac-arm64
echo.

pause
endlocal
