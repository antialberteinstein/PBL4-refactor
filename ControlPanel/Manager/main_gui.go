//go:build gui
// +build gui

package main

import (
    "fmt"
    "os"
    "os/exec"
    "path/filepath"
    "runtime"
    "strings"
    "strconv"
    "time"

    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/app"
    "fyne.io/fyne/v2/container"
    "fyne.io/fyne/v2/dialog"
    "fyne.io/fyne/v2/widget"
)

const (
    appName    = "Manager Control Panel"
    appVersion = "1.0.0 GUI"
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

// runScript executes a shell script and returns output
func runScript(scriptPath string, outputCallback func(string)) error {
    if !checkFileExists(scriptPath) {
        return fmt.Errorf("script not found: %s", scriptPath)
    }

    var cmd *exec.Cmd
    if runtime.GOOS == "windows" {
        cmd = exec.Command("cmd", "/c", scriptPath)
    } else {
        cmd = exec.Command("/bin/bash", scriptPath)
    }

    output, err := cmd.CombinedOutput()
    if outputCallback != nil {
        outputCallback(string(output))
    }
    return err
}

// runScriptInTerminal opens a new terminal window to run the script
func runScriptInTerminal(scriptPath string) error {
    if !checkFileExists(scriptPath) {
        return fmt.Errorf("script not found: %s", scriptPath)
    }

    var cmd *exec.Cmd
    switch runtime.GOOS {
    case "darwin":
        cmd = exec.Command("osascript", "-e",
            fmt.Sprintf(`tell application "Terminal" to do script "%s"`, scriptPath))
    case "linux":
        terminals := []string{"gnome-terminal", "konsole", "xterm"}
        for _, term := range terminals {
            if _, err := exec.LookPath(term); err == nil {
                if term == "gnome-terminal" {
                    cmd = exec.Command(term, "--", "bash", scriptPath)
                } else {
                    cmd = exec.Command(term, "-e", "bash "+scriptPath)
                }
                break
            }
        }
        if cmd == nil {
            return fmt.Errorf("no terminal emulator found")
        }
    case "windows":
        cmd = exec.Command("cmd", "/c", "start", "cmd", "/k", scriptPath)
    }

    return cmd.Start()
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

// marker helpers
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

func readPid() (int, error) {
    execDir, _ := getExecutableDir()
    p := filepath.Join(execDir, ".manager_pid")
    b, err := os.ReadFile(p)
    if err != nil {
        return 0, err
    }
    return strconv.Atoi(strings.TrimSpace(string(b)))
}

func writePid(pid int) error {
    execDir, _ := getExecutableDir()
    p := filepath.Join(execDir, ".manager_pid")
    return os.WriteFile(p, []byte(strconv.Itoa(pid)), 0644)
}

func main() {
    myApp := app.New()
    myWin := myApp.NewWindow(appName + " - " + appVersion)

    status := widget.NewLabel("Ready")

    // Buttons (declare first so closures can reference them)
    var runManagerGUIBtn *widget.Button
    var installDeployBtn *widget.Button
    var startWebBtn *widget.Button

    // Buttons
    installJavaBtn := widget.NewButton("📦 Install Java", func() {
        status.SetText("Installing Java...")
        scriptPath := getScriptPath("install_java")
        // run synchronously on UI thread (may block while installing)
        err := runScript(scriptPath, func(output string) {
            dialog.ShowInformation("Java Installation", output, myWin)
        })
        if err != nil {
            dialog.ShowError(err, myWin)
            status.SetText("Error installing Java: " + err.Error())
        } else {
            // write marker if java now present
            javaInstalled, _ := checkJavaVersion()
            if javaInstalled {
                _ = writeMarker("java_ok")
                status.SetText("Java installed")
                if runManagerGUIBtn != nil {
                    runManagerGUIBtn.Enable()
                }
            } else {
                status.SetText("Java not detected after install")
            }
        }
    })

    runManagerGUIBtn = widget.NewButton("🖥️ Run Manager (GUI)", func() {
        status.SetText("Starting Manager GUI...")
        scriptPath := getScriptPath("run_manager_gui")
        // start in a terminal; this returns quickly on most platforms
        err := runScriptInTerminal(scriptPath)
        if err != nil {
            dialog.ShowError(err, myWin)
            status.SetText("Error: " + err.Error())
            return
        }
        _ = writeMarker("manager_running")
        if installDeployBtn != nil {
            installDeployBtn.Enable()
        }
        runManagerGUIBtn.Disable()
        status.SetText("Manager GUI started")
    })

    installDeployBtn = widget.NewButton("📥 Install Web Deployment", func() {
        status.SetText("Installing web deployment...")
        scriptPath := getScriptPath("install_deploy")
        // run synchronously (may block)
        err := runScript(scriptPath, func(output string) {
            dialog.ShowInformation("Install Deploy", output, myWin)
        })
        if err != nil {
            dialog.ShowError(err, myWin)
            status.SetText("Error: " + err.Error())
        } else {
            _ = writeMarker("deploy_installed")
            if startWebBtn != nil {
                startWebBtn.Enable()
            }
            installDeployBtn.Disable()
            status.SetText("Web deployment installed")
        }
    })

    startWebBtn = widget.NewButton("🌐 Start Web Server", func() {
        status.SetText("Starting web server...")
        scriptPath := getScriptPath("start_web")
        // start web server in terminal (returns quickly)
        err := runScriptInTerminal(scriptPath)
        if err != nil {
            dialog.ShowError(err, myWin)
            status.SetText("Error: " + err.Error())
            return
        }
        _ = writeMarker("web_running")
        startWebBtn.Disable()
        status.SetText("Web server started")
    })

    // Info buttons
    checkStatusBtn := widget.NewButton("🔍 Check Status", func() {
        javaInstalled, version := checkJavaVersion()
        execDir, _ := getExecutableDir()
        jarPath := filepath.Join(execDir, "Manager.jar")
        jarExists := checkFileExists(jarPath)

        info := fmt.Sprintf("Java: %s\n", version)
        if !javaInstalled {
            info = "Java: Not installed\n"
        }
        info += fmt.Sprintf("Manager.jar: %v\n", jarExists)
        info += fmt.Sprintf("Deploy installed: %v\n", checkMarker("deploy_installed"))
        info += fmt.Sprintf("Manager running: %v\n", checkMarker("manager_running"))

        dialog.ShowInformation("Status", info, myWin)
        status.SetText("Status checked")
    })

    viewScriptsBtn := widget.NewButton("📄 View Scripts Info", func() {
        execDir, _ := getExecutableDir()
        scripts := []string{"install_java", "run_manager_gui", "install_deploy", "start_web"}
        info := "Available Scripts:\n\n"
        for _, script := range scripts {
            scriptPath := getScriptPath(script)
            if checkFileExists(scriptPath) {
                info += fmt.Sprintf("✓ %s\n", filepath.Base(scriptPath))
            } else {
                info += fmt.Sprintf("✗ %s (not found)\n", filepath.Base(scriptPath))
            }
        }
        info += fmt.Sprintf("\nLocation: %s", execDir)
        dialog.ShowInformation("Scripts", info, myWin)
    })

    // Initial enable/disable state based on markers
    if !checkMarker("java_ok") {
        runManagerGUIBtn.Disable()
    }
    if !checkMarker("manager_running") {
        installDeployBtn.Disable()
    }
    if !checkMarker("deploy_installed") {
        startWebBtn.Disable()
    }

    content := container.NewVBox(
        widget.NewLabel("🚀 Manager Control Panel (GUI)"),
        widget.NewSeparator(),
        widget.NewLabel("Setup:"),
        installJavaBtn,
        widget.NewSeparator(),
        widget.NewLabel("Run Manager (GUI):"),
        runManagerGUIBtn,
        widget.NewSeparator(),
        widget.NewLabel("Web Server:"),
        installDeployBtn,
        startWebBtn,
        widget.NewSeparator(),
        widget.NewLabel("Information:"),
        checkStatusBtn,
        viewScriptsBtn,
        widget.NewSeparator(),
        status,
    )

    myWin.SetContent(content)
    myWin.Resize(fyne.NewSize(420, 520))
    myWin.ShowAndRun()
}
