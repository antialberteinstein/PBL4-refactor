//go:build cli
// +build cli

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
	"syscall"
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

// runScript executes a shell script
func runScript(scriptPath string) error {
	if !checkFileExists(scriptPath) {
		return fmt.Errorf("script not found: %s", scriptPath)
	}

	var cmd *exec.Cmd
	if runtime.GOOS == "windows" {
		cmd = exec.Command("cmd", "/c", scriptPath)
	} else {
		cmd = exec.Command("/bin/bash", scriptPath)
	}

	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	cmd.Stdin = os.Stdin

	return cmd.Run()
}

// checkJavaVersion checks if Java is installed
func checkJavaVersion() (bool, string) {
	cmd := exec.Command("java", "-version")
	output, err := cmd.CombinedOutput()
	if err != nil {
		return false, ""
	}

	outputStr := string(output)
	if strings.Contains(outputStr, "version") {
		lines := strings.Split(outputStr, "\n")
		if len(lines) > 0 {
			return true, strings.TrimSpace(lines[0])
		}
	}
	return true, "Unknown version"
}

// printHeader prints the application header
func printHeader() {
	clearScreen()
	fmt.Printf("%s", colorCyan)
	fmt.Println("╔════════════════════════════════════════╗")
	fmt.Printf("║  %-36s  ║\n", appName)
	fmt.Printf("║  %-36s  ║\n", "Version: "+appVersion)
	fmt.Println("╚════════════════════════════════════════╝")
	fmt.Printf("%s\n", colorReset)
}

// clearScreen clears the console screen
func clearScreen() {
	if runtime.GOOS == "windows" {
		cmd := exec.Command("cmd", "/c", "cls")
		cmd.Stdout = os.Stdout
		cmd.Run()
	} else {
		cmd := exec.Command("clear")
		cmd.Stdout = os.Stdout
		cmd.Run()
	}
}

// printMenu displays the main menu
func printMenu() {
	fmt.Printf("%s", colorWhite)
	fmt.Println("\n┌─────────────────────────────────────┐")
	fmt.Println("│         MAIN MENU (CLI)             │")
	fmt.Println("├─────────────────────────────────────┤")
	fmt.Println("│ Setup:                              │")
	fmt.Printf("│ %s1.%s %-30s │\n", colorGreen, colorWhite, "Install Java")
	fmt.Println("│                                     │")
	fmt.Println("│ Run Manager (CLI Mode):             │")
	fmt.Printf("│ %s2.%s %-30s │\n", colorGreen, colorWhite, "Run via Script (foreground)")
	fmt.Printf("│ %s3.%s %-30s │\n", colorGreen, colorWhite, "Run via Script (background)")
	fmt.Printf("│ %s4.%s %-30s │\n", colorRed, colorWhite, "Stop background run")
	fmt.Printf("│ %s5.%s %-30s │\n", colorCyan, colorWhite, "Run JAR Direct")
	fmt.Println("│                                     │")
	fmt.Println("│ Web Server:                         │")
	fmt.Printf("│ %s6.%s %-30s │\n", colorPurple, colorWhite, "Install Web Deployment")
	fmt.Printf("│ %s7.%s %-30s │\n", colorPurple, colorWhite, "Start Web Server")
	fmt.Println("│                                     │")
	fmt.Println("│ Info:                               │")
	fmt.Printf("│ %s8.%s %-30s │\n", colorYellow, colorWhite, "Check Status")
	fmt.Printf("│ %s9.%s %-30s │\n", colorYellow, colorWhite, "View Scripts Info")
	fmt.Println("│                                     │")
	fmt.Printf("│ %s0.%s %-30s │\n", colorRed, colorWhite, "Exit")
	fmt.Println("└─────────────────────────────────────┘")
	fmt.Printf("%s\n", colorReset)
}

// isPidRunning returns true if a process with the given pid appears to be running
func isPidRunning(pid int) bool {
	if pid <= 0 {
		return false
	}
	proc, err := os.FindProcess(pid)
	if err != nil {
		return false
	}
	// On Unix, signal 0 can be used to test for existence
	if runtime.GOOS != "windows" {
		err = proc.Signal(syscall.Signal(0))
		return err == nil
	}
	// On Windows try to send a zero signal (may not be supported) — fallback to true if FindProcess succeeded
	err = proc.Signal(syscall.Signal(0))
	return err == nil
}

// runManagerCLIBackground starts the Manager CLI script in background and records its PID
func runManagerCLIBackground() {
	fmt.Printf("%s\n=== Starting Manager (CLI Mode) in background ===%s\n\n", colorYellow, colorReset)

	// Check Java
	javaInstalled, _ := checkJavaVersion()
	if !javaInstalled && !checkMarker("java_ok") {
		fmt.Printf("%sError: Java is not installed. Please install Java first (option 1).%s\n", colorRed, colorReset)
		fmt.Println("Press Enter to continue...")
		fmt.Scanln()
		return
	}

	// if manager already marked running, refuse
	if checkMarker("manager_running") {
		if pid, err := readPid(); err == nil {
			if isPidRunning(pid) {
				fmt.Printf("%sManager already running (pid=%d).%s\n", colorYellow, pid, colorReset)
				fmt.Println("Press Enter to continue...")
				fmt.Scanln()
				return
			}
		}
	}

	scriptPath := getScriptPath("run_manager_cli")
	if scriptPath == "" {
		fmt.Printf("%sError: Could not determine script path%s\n", colorRed, colorReset)
		return
	}

	var cmd *exec.Cmd
	if runtime.GOOS == "windows" {
		cmd = exec.Command("cmd", "/c", scriptPath)
	} else {
		cmd = exec.Command("/bin/bash", scriptPath)
	}

	// start and detach
	err := cmd.Start()
	if err != nil {
		fmt.Printf("%sError starting background Manager: %v%s\n", colorRed, err, colorReset)
		fmt.Println("Press Enter to continue...")
		fmt.Scanln()
		return
	}

	pid := cmd.Process.Pid
	_ = writePid(pid)
	_ = writeMarker("manager_running")
	fmt.Printf("Manager started in background (pid=%d)\n", pid)
	fmt.Println("Press Enter to continue...")
	fmt.Scanln()
}

// stopManagerBackground stops the background Manager process if found
func stopManagerBackground() {
	fmt.Printf("%s\n=== Stop Manager Background ===%s\n\n", colorYellow, colorReset)
	pid, err := readPid()
	if err != nil {
		fmt.Printf("%sNo background pid found.%s\n", colorYellow, colorReset)
		fmt.Println("Press Enter to continue...")
		fmt.Scanln()
		return
	}
	proc, err := os.FindProcess(pid)
	if err != nil {
		fmt.Printf("%sCould not find process: %v%s\n", colorRed, err, colorReset)
		_ = removeMarker("manager_running")
		_ = os.Remove(filepath.Join(getMarkerDir(), ".manager_pid"))
		fmt.Println("Press Enter to continue...")
		fmt.Scanln()
		return
	}
	// try kill
	if runtime.GOOS == "windows" {
		err = proc.Kill()
	} else {
		err = proc.Signal(syscall.SIGTERM)
	}
	if err != nil {
		fmt.Printf("%sError stopping process %d: %v%s\n", colorRed, pid, err, colorReset)
	} else {
		fmt.Printf("Stopped process %d\n", pid)
	}
	_ = removeMarker("manager_running")
	// remove pid file
	execDir, _ := getExecutableDir()
	_ = os.Remove(filepath.Join(execDir, ".manager_pid"))
	fmt.Println("Press Enter to continue...")
	fmt.Scanln()
}

// helper to get marker dir (executable dir)
func getMarkerDir() string {
	d, _ := getExecutableDir()
	return d
}

// installJava runs the Java installation script
func installJava() {
	fmt.Printf("%s\n=== Installing Java ===%s\n\n", colorYellow, colorReset)
	scriptPath := getScriptPath("install_java")
	
	if scriptPath == "" {
		fmt.Printf("%sError: Could not determine script path%s\n", colorRed, colorReset)
		return
	}

	err := runScript(scriptPath)
	if err != nil {
		fmt.Printf("%sError running install script: %v%s\n", colorRed, err, colorReset)
	}
	// after attempted install, verify Java and write marker if ok
	javaInstalled, _ := checkJavaVersion()
	if javaInstalled {
		_ = writeMarker("java_ok")
		fmt.Println("Java appears to be installed.")
	} else {
		fmt.Println("Java still not detected. If installation succeeded, try restarting this control panel.")
	}

	fmt.Println("\nPress Enter to continue...")
	fmt.Scanln()
}

// runManagerCLI runs the Manager in CLI mode
func runManagerCLI() {
	fmt.Printf("%s\n=== Running Manager (CLI Mode) ===%s\n\n", colorYellow, colorReset)
	// enforce Java installed
	javaInstalled, _ := checkJavaVersion()
	if !javaInstalled && !checkMarker("java_ok") {
		fmt.Printf("%sError: Java is not installed. Please install Java first (option 1).%s\n", colorRed, colorReset)
		fmt.Println("Press Enter to continue...")
		fmt.Scanln()
		return
	}

	scriptPath := getScriptPath("run_manager_cli")
	if scriptPath == "" {
		fmt.Printf("%sError: Could not determine script path%s\n", colorRed, colorReset)
		return
	}

	// mark running
	_ = writeMarker("manager_running")
	fmt.Println("Starting Manager (foreground). Press Ctrl+C to stop.")
	err := runScript(scriptPath)
	if err != nil {
		fmt.Printf("%sError running Manager: %v%s\n", colorRed, err, colorReset)
	}
	// clear running marker after exit
	_ = removeMarker("manager_running")
	fmt.Println("Manager (foreground) exited.")
	fmt.Println("\nPress Enter to continue...")
	fmt.Scanln()
}

// runManagerGUI runs the Manager in GUI mode
func runManagerGUI() {
	fmt.Printf("%s\n=== Running Manager (GUI Mode) ===%s\n\n", colorYellow, colorReset)
	scriptPath := getScriptPath("run_manager_gui")

	if scriptPath == "" {
		fmt.Printf("%sError: Could not determine script path%s\n", colorRed, colorReset)
		return
	}

	// start GUI in new terminal/window
	// try starting GUI in a terminal; provide a minimal implementation
	err := runScriptInTerminal(scriptPath)
	if err != nil {
		fmt.Printf("%sError running Manager GUI: %v%s\n", colorRed, err, colorReset)
		fmt.Println("\nPress Enter to continue...")
		fmt.Scanln()
		return
	}

	// optimistic: mark manager as running so next steps enabled
	_ = writeMarker("manager_running")
	fmt.Println("Manager GUI started (marker created).")
	fmt.Println("\nPress Enter to continue...")
	fmt.Scanln()
}

// runScriptInTerminal tries to open a new terminal window to run the script.
// This is a minimal implementation used by the CLI build so the package
// compiles even when the GUI file (which has a fancier implementation)
// is excluded by build tags.
func runScriptInTerminal(scriptPath string) error {
	if !checkFileExists(scriptPath) {
		return fmt.Errorf("script not found: %s", scriptPath)
	}

	var cmd *exec.Cmd
	switch runtime.GOOS {
	case "darwin":
		// use osascript to open Terminal and run the script
		cmd = exec.Command("osascript", "-e", fmt.Sprintf(`tell application "Terminal" to do script "%s"`, scriptPath))
		return cmd.Start()
	case "linux":
		// fallback: try to run directly (no terminal) which is acceptable for CLI
		cmd = exec.Command("/bin/bash", scriptPath)
		cmd.Stdout = os.Stdout
		cmd.Stderr = os.Stderr
		return cmd.Start()
	case "windows":
		cmd = exec.Command("cmd", "/c", "start", "cmd", "/k", scriptPath)
		return cmd.Start()
	default:
		return fmt.Errorf("unsupported OS for opening a terminal: %s", runtime.GOOS)
	}
}

// runJarDirectCLI runs the JAR file directly in CLI mode
func runJarDirectCLI() {
	fmt.Printf("%s\n=== Running Manager JAR Direct (CLI Mode) ===%s\n\n", colorYellow, colorReset)
	
	execDir, _ := getExecutableDir()
	jarPath := filepath.Join(execDir, "Manager.jar")
	
	if !checkFileExists(jarPath) {
		fmt.Printf("%sError: Manager.jar not found at %s%s\n", colorRed, jarPath, colorReset)
		fmt.Println("\nPress Enter to continue...")
		fmt.Scanln()
		return
	}

	// Check Java
	javaInstalled, _ := checkJavaVersion()
	if !javaInstalled {
		fmt.Printf("%sError: Java is not installed%s\n", colorRed, colorReset)
		fmt.Println("Please install Java first (option 1)")
		fmt.Println("\nPress Enter to continue...")
		fmt.Scanln()
		return
	}

	fmt.Println("Starting Manager in CLI mode...")
	fmt.Println("Press Ctrl+C to stop.")
	fmt.Println()

	cmd := exec.Command("java", "-jar", jarPath)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	cmd.Stdin = os.Stdin

	err := cmd.Run()
	if err != nil {
		fmt.Printf("%sError running Manager: %v%s\n", colorRed, err, colorReset)
		fmt.Println("\nPress Enter to continue...")
		fmt.Scanln()
	}
}

// runJarDirectGUI runs the JAR file directly in GUI mode
func runJarDirectGUI() {
	fmt.Printf("%s\n=== Running Manager JAR Direct (GUI Mode) ===%s\n\n", colorYellow, colorReset)
	
	execDir, _ := getExecutableDir()
	jarPath := filepath.Join(execDir, "Manager.jar")
	
	if !checkFileExists(jarPath) {
		fmt.Printf("%sError: Manager.jar not found at %s%s\n", colorRed, jarPath, colorReset)
		fmt.Println("\nPress Enter to continue...")
		fmt.Scanln()
		return
	}

	// Check Java
	javaInstalled, _ := checkJavaVersion()
	if !javaInstalled {
		fmt.Printf("%sError: Java is not installed%s\n", colorRed, colorReset)
		fmt.Println("Please install Java first (option 1)")
		fmt.Println("\nPress Enter to continue...")
		fmt.Scanln()
		return
	}

	fmt.Println("Starting Manager in GUI mode...")
	fmt.Println("Press Ctrl+C to stop.")
	fmt.Println()

	cmd := exec.Command("java", "-jar", jarPath, "--gui")
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	cmd.Stdin = os.Stdin

	err := cmd.Run()
	if err != nil {
		fmt.Printf("%sError running Manager: %v%s\n", colorRed, err, colorReset)
		fmt.Println("\nPress Enter to continue...")
		fmt.Scanln()
	}
}

// installWebDeployment installs Jetty and deploys the web application
func installWebDeployment() {
	fmt.Printf("%s\n=== Installing Web Deployment ===%s\n\n", colorYellow, colorReset)
	scriptPath := getScriptPath("install_deploy")
	
	if scriptPath == "" {
		fmt.Printf("%sError: Could not determine script path%s\n", colorRed, colorReset)
		return
	}

	fmt.Println("This will download and install Jetty web server")
	fmt.Println("and deploy the ManagerWeb application.")
	fmt.Println()

	err := runScript(scriptPath)
	if err != nil {
		fmt.Printf("%sError running install deployment: %v%s\n", colorRed, err, colorReset)
	}
	
	fmt.Println("\nPress Enter to continue...")
	fmt.Scanln()
}

// startWebServer starts the Jetty web server
func startWebServer() {
	fmt.Printf("%s\n=== Starting Web Server ===%s\n\n", colorYellow, colorReset)
	scriptPath := getScriptPath("start_web")
	
	if scriptPath == "" {
		fmt.Printf("%sError: Could not determine script path%s\n", colorRed, colorReset)
		return
	}

	fmt.Println("Starting Jetty web server...")
	fmt.Println("The web interface will be available at: http://localhost:8080/")
	fmt.Println("Press Ctrl+C to stop the server.")
	fmt.Println()

	err := runScript(scriptPath)
	if err != nil {
		fmt.Printf("%sError starting web server: %v%s\n", colorRed, err, colorReset)
		fmt.Println("\nPress Enter to continue...")
		fmt.Scanln()
	}
}

// checkJavaStatus checks and displays Java installation status
func checkJavaStatus() {
	fmt.Printf("%s\n=== Java Status ===%s\n\n", colorYellow, colorReset)
	
	javaInstalled, version := checkJavaVersion()
	
	if javaInstalled {
		fmt.Printf("%s✓ Java is installed%s\n", colorGreen, colorReset)
		fmt.Printf("  Version: %s\n", version)
	} else {
		fmt.Printf("%s✗ Java is not installed%s\n", colorRed, colorReset)
		fmt.Println("\nPlease run 'Install Java' from the menu.")
	}
	
	// Check if Manager files exist
	execDir, _ := getExecutableDir()
	
	fmt.Printf("\n%s=== Manager Files ===%s\n", colorYellow, colorReset)
	
	// Check Manager.jar
	jarPath := filepath.Join(execDir, "Manager.jar")
	if checkFileExists(jarPath) {
		fmt.Printf("%s✓ Manager.jar found%s\n", colorGreen, colorReset)
		fmt.Printf("  Location: %s\n", jarPath)
	} else {
		fmt.Printf("%s✗ Manager.jar not found%s\n", colorRed, colorReset)
		fmt.Printf("  Expected location: %s\n", jarPath)
	}
	
	// Check ManagerWeb.war
	warPath := filepath.Join(execDir, "ManagerWeb.war")
	if checkFileExists(warPath) {
		fmt.Printf("%s✓ ManagerWeb.war found%s\n", colorGreen, colorReset)
		fmt.Printf("  Location: %s\n", warPath)
	} else {
		fmt.Printf("%s✗ ManagerWeb.war not found%s\n", colorRed, colorReset)
		fmt.Printf("  Expected location: %s\n", warPath)
	}
	
	// Check Jetty installation
	jettyPath := filepath.Join(execDir, "jetty_server")
	if checkFileExists(jettyPath) {
		fmt.Printf("%s✓ Jetty server installed%s\n", colorGreen, colorReset)
		fmt.Printf("  Location: %s\n", jettyPath)
	} else {
		fmt.Printf("%s✗ Jetty server not installed%s\n", colorYellow, colorReset)
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
			runManagerCLIBackground()
		case "4":
			stopManagerBackground()
		case "5":
			runJarDirectCLI()
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
