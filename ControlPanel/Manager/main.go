//go:build ignore
// +build ignore

package main

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"time"
	"strconv"
)

const (
	appName    = "Manager Control Panel"
	appVersion = "1.0.0"
)

// ANSI color codes
const (
	colorReset  = "\033[0m"
	colorRed    = "\033[31m"
	colorGreen  = "\033[32m"
	colorYellow = "\033[33m"
	colorBlue   = "\033[34m"
	colorPurple = "\033[35m"
	colorCyan   = "\033[36m"
	colorWhite  = "\033[37m"
)

// getExecutableDir returns the directory where this executable is located
func getExecutableDir() (string, error) {
	exe, err := os.Executable()
	if err != nil {
		return "", err
	}
	return filepath.Dir(exe), nil
}

// getScriptPath returns the full path to a script file based on OS
func getScriptPath(scriptName string) string {
	execDir, err := getExecutableDir()
	if err != nil {
		fmt.Printf("Error getting executable directory: %v\n", err)
		return ""
	}

	if runtime.GOOS == "windows" {
		return filepath.Join(execDir, scriptName+".bat")
	}
	return filepath.Join(execDir, scriptName+".sh")
}

// checkFileExists checks if a file exists
func checkFileExists(path string) bool {
	_, err := os.Stat(path)
	return err == nil
}

// marker helpers (store simple files in exec dir to remember state)
func markerPath(name string) string {
	execDir, _ := getExecutableDir()
	return filepath.Join(execDir, "."+name)
}

func writeMarker(name string) error {
	p := markerPath(name)
	return os.WriteFile(p, []byte(time.Now().Format(time.RFC3339)), 0644)
}

func removeMarker(name string) error {
	p := markerPath(name)
	_ = os.Remove(p)
	return nil
}

func checkMarker(name string) bool {
	p := markerPath(name)
	_, err := os.Stat(p)
	return err == nil
}

func writePid(pid int) error {
	execDir, _ := getExecutableDir()
	p := filepath.Join(execDir, ".manager_pid")
	return os.WriteFile(p, []byte(strconv.Itoa(pid)), 0644)
}

func readPid() (int, error) {
	execDir, _ := getExecutableDir()
	p := filepath.Join(execDir, ".manager_pid")
	b, err := os.ReadFile(p)
	if err != nil {
		return 0, err
	}
	return strconv.Atoi(strings.TrimSpace(string(b)))
}

		fmt.Println("  Run 'Install Web Deployment' to set up Jetty")
	}
	
	fmt.Println("\nPress Enter to continue...")
	fmt.Scanln()
}

// viewScriptsInfo displays information about available scripts
func viewScriptsInfo() {
	fmt.Printf("%s\n=== Scripts Information ===%s\n\n", colorYellow, colorReset)
	
	execDir, _ := getExecutableDir()
	fmt.Printf("Executable Directory: %s\n", execDir)
	fmt.Printf("Operating System: %s\n", runtime.GOOS)
	fmt.Printf("Architecture: %s\n\n", runtime.GOARCH)
	
	scripts := []string{"install_java", "run_manager", "install_deploy", "start_web"}
	
	fmt.Println("Available Scripts:")
	fmt.Println("─────────────────────────────────────")
	
	for _, script := range scripts {
		scriptPath := getScriptPath(script)
		exists := checkFileExists(scriptPath)
		
		if exists {
			fmt.Printf("%s✓%s %s\n", colorGreen, colorReset, filepath.Base(scriptPath))
			fmt.Printf("  Path: %s\n", scriptPath)
		} else {
			fmt.Printf("%s✗%s %s (NOT FOUND)\n", colorRed, colorReset, filepath.Base(scriptPath))
			fmt.Printf("  Expected: %s\n", scriptPath)
		}
		fmt.Println()
	}
	
	fmt.Println("Press Enter to continue...")
	fmt.Scanln()
}

// main function
func main() {
	var choice string

	for {
		printHeader()
		
		// Show Java status at top
		javaInstalled, version := checkJavaVersion()
		if javaInstalled {
			fmt.Printf("%sJava Status:%s %s✓ Installed%s (%s)\n", 
				colorCyan, colorReset, colorGreen, colorReset, version)
		} else {
			fmt.Printf("%sJava Status:%s %s✗ Not Installed%s\n", 
				colorCyan, colorReset, colorRed, colorReset)
		}
		
		printMenu()
		
		fmt.Print("Enter your choice: ")
		fmt.Scanln(&choice)
		
		switch choice {
		case "1":
			installJava()
		case "2":
			runManagerCLI()
		case "3":
			runManagerGUI()
		case "4":
			runJarDirectCLI()
		case "5":
			runJarDirectGUI()
		case "6":
			installWebDeployment()
		case "7":
			startWebServer()
		case "8":
			checkJavaStatus()
		case "9":
			viewScriptsInfo()
		case "0":
			fmt.Printf("\n%sThank you for using %s!%s\n", colorGreen, appName, colorReset)
			os.Exit(0)
		default:
			fmt.Printf("%sInvalid choice. Please try again.%s\n", colorRed, colorReset)
			fmt.Println("Press Enter to continue...")
			fmt.Scanln()
		}
	}
}
