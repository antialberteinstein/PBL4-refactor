#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "  Building Manager Control Panel"
echo "=========================================="
echo ""

# Get dependencies first
echo "→ Getting dependencies..."
go get fyne.io/fyne/v2@v2.4.5
go mod tidy

OUTPUT_DIR="../../Deploy/Manager"
mkdir -p "$OUTPUT_DIR"

echo ""
echo "Building for multiple platforms..."
echo ""

# Build CLI versions
echo "→ Building CLI for Windows (amd64)..."
GOOS=windows GOARCH=amd64 go build -tags=cli -o "$OUTPUT_DIR/ManagerControlPanel-CLI.exe" main_cli.go
echo -e "  ${GREEN}✓${NC} Created: ManagerControlPanel-CLI.exe"

echo "→ Building CLI for Linux (amd64)..."
GOOS=linux GOARCH=amd64 go build -tags=cli -o "$OUTPUT_DIR/ManagerControlPanel-CLI" main_cli.go
chmod +x "$OUTPUT_DIR/ManagerControlPanel-CLI"
echo -e "  ${GREEN}✓${NC} Created: ManagerControlPanel-CLI (Linux)"

echo "→ Building CLI for macOS (amd64)..."
GOOS=darwin GOARCH=amd64 go build -tags=cli -o "$OUTPUT_DIR/ManagerControlPanel-CLI-mac" main_cli.go
chmod +x "$OUTPUT_DIR/ManagerControlPanel-CLI-mac"
echo -e "  ${GREEN}✓${NC} Created: ManagerControlPanel-CLI-mac (Intel)"

echo "→ Building CLI for macOS (arm64)..."
GOOS=darwin GOARCH=arm64 go build -tags=cli -o "$OUTPUT_DIR/ManagerControlPanel-CLI-mac-arm64" main_cli.go
chmod +x "$OUTPUT_DIR/ManagerControlPanel-CLI-mac-arm64"
echo -e "  ${GREEN}✓${NC} Created: ManagerControlPanel-CLI-mac-arm64 (Apple Silicon)"

echo ""
echo "→ Building GUI for Windows (amd64)..."
GOOS=windows GOARCH=amd64 go build -tags=gui -o "$OUTPUT_DIR/ManagerControlPanel-GUI.exe" main_gui.go
echo -e "  ${GREEN}✓${NC} Created: ManagerControlPanel-GUI.exe"

echo "→ Building GUI for Linux (amd64)..."
GOOS=linux GOARCH=amd64 go build -tags=gui -o "$OUTPUT_DIR/ManagerControlPanel-GUI" main_gui.go
chmod +x "$OUTPUT_DIR/ManagerControlPanel-GUI"
echo -e "  ${GREEN}✓${NC} Created: ManagerControlPanel-GUI (Linux)"

echo "→ Building GUI for macOS (amd64)..."
GOOS=darwin GOARCH=amd64 go build -tags=gui -o "$OUTPUT_DIR/ManagerControlPanel-GUI-mac" main_gui.go
chmod +x "$OUTPUT_DIR/ManagerControlPanel-GUI-mac"
echo -e "  ${GREEN}✓${NC} Created: ManagerControlPanel-GUI-mac (Intel)"

echo "→ Building GUI for macOS (arm64)..."
GOOS=darwin GOARCH=arm64 go build -tags=gui -o "$OUTPUT_DIR/ManagerControlPanel-GUI-mac-arm64" main_gui.go
chmod +x "$OUTPUT_DIR/ManagerControlPanel-GUI-mac-arm64"
echo -e "  ${GREEN}✓${NC} Created: ManagerControlPanel-GUI-mac-arm64 (Apple Silicon)"

echo ""
echo "=========================================="
echo "  Build Complete!"
echo "=========================================="
echo ""
echo "Output files created in: $OUTPUT_DIR"
echo ""
echo "CLI Files:"
echo "  - ManagerControlPanel-CLI.exe        (Windows)"
echo "  - ManagerControlPanel-CLI            (Linux)"
echo "  - ManagerControlPanel-CLI-mac        (macOS Intel)"
echo "  - ManagerControlPanel-CLI-mac-arm64  (macOS ARM)"
echo ""
echo "GUI Files:"
echo "  - ManagerControlPanel-GUI.exe        (Windows)"
echo "  - ManagerControlPanel-GUI            (Linux)"
echo "  - ManagerControlPanel-GUI-mac        (macOS Intel)"
echo "  - ManagerControlPanel-GUI-mac-arm64  (macOS ARM)"
echo ""
echo "To run:"
echo "  CLI Windows: ManagerControlPanel-CLI.exe"
echo "  CLI Linux:   ./ManagerControlPanel-CLI"
echo "  CLI macOS:   ./ManagerControlPanel-CLI-mac or ./ManagerControlPanel-CLI-mac-arm64"
echo ""
echo "  GUI Windows: ManagerControlPanel-GUI.exe"
echo "  GUI Linux:   ./ManagerControlPanel-GUI"
echo "  GUI macOS:   ./ManagerControlPanel-GUI-mac or ./ManagerControlPanel-GUI-mac-arm64"
