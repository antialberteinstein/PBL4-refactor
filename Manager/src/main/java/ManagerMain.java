import java.util.Scanner;

import cli.CliInterface;
import cli.CommandMode;
import config.AppConfig;
import config.ConfigManager;
import database.AuthRepository;
import database.ComputerManager;
import database.DatabaseManager;
import database.ProcessManager;
import database.SessionManager;
import service.*;
import util.Messages;
import util.ProtocolManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ManagerMain - Entry point for Manager application
 * 
 * The Manager is responsible for:
 * 1. Discovering Agent computers on the network
 * 2. Collecting system monitoring data from Agents
 * 3. Storing collected data in centralized database
 * 4. Providing data for analysis/visualization (future UI)
 * 
 * Architecture Pattern: Dependency Injection + Multi-threading + Observer Pattern
 * - All dependencies are injected through constructor
 * - One thread per Agent for continuous data collection
 * - User-triggered network scanning for Agent discovery
 * - Implements AgentDiscoveryListener to respond to newly discovered Agents
 */
public class ManagerMain implements AgentDiscoveryListener {

    // ### All dependencies (no static state for better testability)
    private final AppConfig appConfig;
    private final DatabaseManager databaseManager;
    private final ProcessManager processManager;
    private final SessionManager sessionManager;
    private final ComputerManager computerManager;
    private final AuthRepository authRepository;
    private final AuthService authService;
    private final NetworkMessageService networkMessageService;
    private final ProtocolManager protocolManager;
    private final HostScanner hostScanner;
    private final SessionRetriever sessionRetriever;
    private final ExternalScanServer externalScanServer;
    private final List<SessionRetriever.SessionRequest> sessionRequestThreads;
    private final Scanner scanner;

    /**
     * Constructor - Initialize all dependencies in correct order
     * 
     * Dependency Injection Flow:
     * ### Step 1: Load configuration
     * ### Step 2: Initialize database layer (Manager classes)
     * ### Step 3: Initialize network communication services
     * ### Step 4: Initialize Agent discovery and data collection services
     */
    public ManagerMain() {
        // ### Step 1: Load configuration (database path, ports, intervals)
        this.appConfig = ConfigManager.loadConfig();
        
        // ### Step 1.5: Initialize language
        Messages.setLanguage(appConfig.getLanguage());
        
        // ### Step 2: Initialize database layer
        // Main database for agents, sessions, processes
        this.databaseManager = new DatabaseManager(appConfig.getDatabaseUrl());
        databaseManager.initializeDatabase(); // MUST initialize tables before any Manager can query DB
        this.processManager = new ProcessManager(databaseManager);
        this.sessionManager = new SessionManager(databaseManager, processManager);
        this.computerManager = new ComputerManager(databaseManager);
        
        // ### Step 2.5: Initialize authentication database (separate from main database)
        DatabaseManager authDatabaseManager = new DatabaseManager(appConfig.getAuthDatabaseUrl());
        authDatabaseManager.initializeDatabase(); // Initialize auth tables
        this.authRepository = new AuthRepository(authDatabaseManager);
        this.authService = new AuthService(authRepository);
        authService.initializeDefaultAdmin(); // Create default admin account if needed
        
        // ### Step 3: Initialize network services
        this.networkMessageService = new NetworkMessageService(appConfig);
        this.protocolManager = new ProtocolManager();
        
        // ### Step 4: Initialize discovery and data collection
        // HostScanner discovers new Agents on network
        this.hostScanner = new HostScanner(appConfig, networkMessageService, protocolManager);
        
        // SessionRetriever requests and stores session data from Agents
        // NOTE: SessionRetriever constructor queries DB, so tables must exist first
        this.sessionRetriever = new SessionRetriever(appConfig, networkMessageService, protocolManager, 
                                                     computerManager, sessionManager, processManager);
        this.sessionRetriever.setAgentDiscoveryListener(this); // Register for Agent discovery callbacks
        
        // ### Step 5: Initialize External Scan Server
        // TCP server that accepts external scan requests (only serves GUI/CLI modes)
        this.externalScanServer = new ExternalScanServer(appConfig, hostScanner);
        
        this.sessionRequestThreads = new ArrayList<>();
        this.scanner = new Scanner(System.in);
    }

    /**
     * Application entry point
     * 
     * Supports multiple modes:
     * - Interactive mode (default): Shows CLI prompt
     * - GUI mode: Opens graphical window
     * - Command mode: Execute single command and exit
     * 
     * Usage:
     *   java ManagerMain                          - Interactive CLI mode
     *   java ManagerMain --gui                    - GUI mode
     *   java ManagerMain --verbose                - Interactive with logs visible
     *   java ManagerMain -c "scan"                - Execute scan command
     *   java ManagerMain --command "list agents"  - Execute query command
     *   java ManagerMain --help                   - Show help
     */
    public static void main(String[] args) throws Exception {
        boolean verboseMode = false;
        
        // Parse command-line arguments BEFORE creating ManagerMain
        // This allows us to enable quiet mode before any logs are generated
        if (args.length > 0) {
            // Check for verbose flag
            for (String arg : args) {
                if (arg.equals("--verbose") || arg.equals("-v")) {
                    verboseMode = true;
                }
            }
            
            if (args[0].equals("--help") || args[0].equals("-h")) {
                showUsage();
                return;
            }
            
            // Check for command mode BEFORE creating ManagerMain (for efficiency)
            if ((args[0].equals("-c") || args[0].equals("--command")) && args.length >= 2) {
                // Command mode - always quiet (no need for logs in single-command execution)
                util.Logger.enableQuietMode();
                
                // Build command from remaining arguments
                StringBuilder commandBuilder = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (i > 1) commandBuilder.append(" ");
                    commandBuilder.append(args[i]);
                }
                String command = commandBuilder.toString();
                
                // Create manager instance (only database initialization needed)
                ManagerMain manager = new ManagerMain();
                
                // Execute command and exit with appropriate exit code
                int exitCode = manager.runCommand(command);
                System.exit(exitCode);
                return;
            }
        }
        
        // Enable quiet mode BEFORE creating ManagerMain instance
        // This prevents logs during constructor execution
        if (!verboseMode) {
            util.Logger.enableQuietMode();
        }
        
        // Now create the ManagerMain instance
        ManagerMain manager = new ManagerMain();
        
        // Register shutdown hook for graceful cleanup on Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n"); // Move to new line
            manager.printGoodbye();
            manager.shutdown();
        }));
        
        // Continue parsing commands
        if (args.length > 0) {
            if (args[0].equals("--gui") || args[0].equals("-g")) {
                // GUI mode
                manager.runGUI();
                return;
            } else if (args[0].equals("--verbose") || args[0].equals("-v")) {
                // Interactive mode with verbose logging
                manager.run(verboseMode);
                return;
            } else if (args[0].equals("--daemon") || args[0].equals("-d")) {
                runDaemon(manager);
                return;
            }
            else if (!args[0].equals("--help") && !args[0].equals("-h")) {
                System.err.println(Messages.get("main.error.unknown.option", args[0]));
                showUsage();
                System.exit(1);
            }
        }
        
        // Interactive mode (default - quiet)
        manager.run(false);
    }

    public static void runDaemon(ManagerMain manager) {
        // This will run with character & to run in background

        // Run scan first.

        // Run the Network Message Service.

        // Run system retrieving.
    }
    
    /**
     * Show command-line usage
     */
    private static void showUsage() {
        System.out.println(Messages.get("main.usage.title"));
        System.out.println();
        System.out.println(Messages.get("main.usage.header"));
        System.out.println(Messages.get("main.usage.interactive"));
        System.out.println(Messages.get("main.usage.gui"));
        System.out.println(Messages.get("main.usage.verbose"));
        System.out.println(Messages.get("main.usage.command.short"));
        System.out.println(Messages.get("main.usage.command.long"));
        System.out.println(Messages.get("main.usage.help"));
        System.out.println();
        System.out.println(Messages.get("main.examples.interactive.title"));
        System.out.println(Messages.get("main.examples.interactive.cli"));
        System.out.println(Messages.get("main.examples.interactive.gui"));
        System.out.println(Messages.get("main.examples.interactive.gui.short"));
        System.out.println(Messages.get("main.examples.interactive.verbose"));
        System.out.println();
        System.out.println(Messages.get("main.examples.command.title"));
        System.out.println(Messages.get("main.examples.command.scan"));
        System.out.println(Messages.get("main.examples.command.agents"));
        System.out.println(Messages.get("main.examples.command.sessions"));
        System.out.println(Messages.get("main.examples.command.help"));
        System.out.println();
        System.out.println(Messages.get("main.notes.title"));
        System.out.println(Messages.get("main.notes.scan"));
        System.out.println(Messages.get("main.notes.other"));
        System.out.println();
    }

    /**
     * Run in command mode (execute single command and exit)
     * 
     * Workflow:
     * ### Step 1: Create CommandMode instance
     * ### Step 2: Execute command
     * ### Step 3: Return exit code
     * 
     * Note: 
     * - No network services are started in command mode
     * - SCAN command connects to ExternalScanServer of running Manager instance
     * - Query commands directly access database
     * 
     * @param command Command to execute (e.g., "scan", "list agents")
     * @return Exit code (0 = success, 1 = error)
     */
    private int runCommand(String command) {
        // ### Step 1: Create CommandMode instance
        CommandMode commandMode = new CommandMode(appConfig, computerManager, sessionManager,
                                                  processManager, hostScanner);
        
        // ### Step 2: Execute command
        int exitCode = commandMode.executeCommand(command);
        
        // ### Step 3: Return exit code (no cleanup needed - no services started)
        return exitCode;
    }
    
    /**
     * Run in interactive CLI mode
     * 
     * Workflow:
     * ### Step 0: Authenticate user
     * ### Step 1: Start network service (UDP listener)
     * ### Step 2: Start data collection threads for known Agents  
     * ### Step 3: Start interactive CLI
     * ### Step 4: Cleanup on exit
     * 
     * @param verbose If true, show background logs. If false, suppress for clean CLI.
     */
    private void run(boolean verbose) throws Exception {
        // ### Step 0: Authenticate user
        if (!performLogin()) {
            System.out.println(Messages.get("main.auth.failed"));
            System.exit(1);
        }
        
        // ### Step 1: Start network service
        // Note: Quiet mode already configured in main() before constructor
        networkMessageService.open();   // Open UDP socket
        networkMessageService.start();  // Start message receiver thread

        // ### Step 2: Start collecting data from known Agents
        startSessionRetrieving();
        
        // ### Step 3: Start External Scan Server (for remote scan requests)
        externalScanServer.start();
        
        // ### Step 4: Start interactive CLI
        CliInterface cli = new CliInterface(computerManager, sessionManager, 
                                           processManager, hostScanner);
        cli.setVerboseMode(verbose); // Enable prompt redrawing in verbose mode
        cli.start();
        
        // ### Step 5: Cleanup (when user types 'exit' or Ctrl+D)
        printGoodbye();
        shutdown();
    }
    

    /**
     * Run in GUI mode
     * 
     * Workflow:
     * ### Step 0: Authenticate user
     * ### Step 1: Start network service (UDP listener)
     * ### Step 2: Start data collection threads for known Agents
     * ### Step 3: Launch GUI window
     * ### Step 4: Keep services running until window is closed
     */
    private void runGUI() throws Exception {
        System.out.println(Messages.get("main.gui.starting"));
        
        // ### macOS-specific configuration for native menu bar
        // These properties MUST be set BEFORE any Swing components are created
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            // Use screen menu bar (menu appears in macOS top bar, not in window)
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            
            // Set application name in menu bar
            System.setProperty("apple.awt.application.name", "Manager");
            
            // Use native macOS look and feel
            try {
                javax.swing.UIManager.setLookAndFeel(
                    javax.swing.UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception e) {
                // Fallback to default look and feel if native fails
                System.err.println("Warning: Could not set native look and feel");
            }
        }
        
        // ### Step 0: Authenticate user with GUI login dialog
        if (!performGUILogin()) {
            System.out.println(Messages.get("main.auth.failed"));
            System.exit(1);
        }
        
        // ### Step 1: Start network service
        networkMessageService.open();   // Open UDP socket
        networkMessageService.start();  // Start message receiver thread
        
        // ### Step 2: Start collecting data from known Agents
        startSessionRetrieving();
        
        // ### Step 3: Start External Scan Server (for remote scan requests)
        externalScanServer.start();
        
        // ### Step 4: Launch GUI window
        javax.swing.SwingUtilities.invokeLater(() -> {
            ui.AgentWindow window = new ui.AgentWindow(
                computerManager, 
                sessionManager, 
                processManager, 
                hostScanner,
                sessionRetriever
            );
            window.setVisible(true);
            
            // Add window close listener to cleanup
            window.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    printGoodbye();
                    shutdown();
                    System.exit(0);
                }
            });
        });
        
        System.out.println(Messages.get("main.gui.launched"));
    }

    
    /**
     * Shutdown all services and threads gracefully
     * 
     * Cleanup workflow:
     * ### Step 1: Stop all SessionRequest threads
     * ### Step 2: Stop network service thread
     * ### Step 3: Close network socket
     * ### Step 4: Close scanner
     */
    private void shutdown() {
        try {
            // ### Step 1: Stop External Scan Server
            System.out.print("Stopping External Scan Server...");
            externalScanServer.shutdown();
            System.out.println(" Done");
            
            // ### Step 2: Stop all SessionRequest threads
            System.out.print("Stopping session collection threads...");
            for (SessionRetriever.SessionRequest thread : sessionRequestThreads) {
                thread.interrupt(); // Signal thread to stop
            }
            
            // Wait for threads to finish (with timeout)
            for (SessionRetriever.SessionRequest thread : sessionRequestThreads) {
                thread.join(1000); // Wait max 1 second per thread
            }
            System.out.println(" Done");
            
            // ### Step 3 & 4: Stop network service
            System.out.print("Stopping network service...");
            networkMessageService.interrupt(); // Stop network thread
            networkMessageService.close();     // Close socket
            System.out.println(" Done");
            
            // ### Step 5: Close scanner
            scanner.close();
            
        } catch (Exception e) {
            System.err.println(Messages.get("main.shutdown.error", e.getMessage()));
        }
    }
    
    /**
     * Print beautiful goodbye message
     */
    private void printGoodbye() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║                                                      ║");
        System.out.println("║              Thank you for using Manager!            ║");
        System.out.println("║                                                      ║");
        System.out.println("║                  See you soon! 👋                    ║");
        System.out.println("║                                                      ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();
    }
    
    
    // ============================================================================== //
    //                            AUTHENTICATION METHODS                              //
    // ============================================================================== //
    
    /**
     * Perform CLI login.
     * 
     * Prompts user for username and password via console.
     * User can type "exit" at username prompt to quit.
     * No limit on login attempts.
     * 
     * @return true if login successful, false if user exits
     */
    private boolean performLogin() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║                                                      ║");
        System.out.println("║       " + Messages.get("auth.cli.header") + "                  ║");
        System.out.println("║                                                      ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("💡 Type 'exit' to quit");
        System.out.println();
        
        while (true) {
            System.out.print(Messages.get("auth.cli.prompt.username"));
            String username = scanner.nextLine().trim();
            
            // Allow user to exit
            if (username.equalsIgnoreCase("exit")) {
                System.out.println();
                System.out.println("Goodbye!");
                return false;
            }
            
            // Validate username not empty
            if (username.isEmpty()) {
                System.out.println();
                System.out.println("✗ " + Messages.get("auth.cli.empty.username"));
                System.out.println();
                continue;
            }
            
            System.out.print(Messages.get("auth.cli.prompt.password"));
            String password = readPassword();
            
            // Attempt login
            if (authService.login(username, password)) {
                System.out.println();
                System.out.println("✓ " + Messages.get("auth.cli.success"));
                System.out.println();
                return true;
            } else {
                System.out.println();
                System.out.println("✗ " + Messages.get("auth.cli.invalid"));
                System.out.println();
            }
        }
    }
    
    /**
     * Perform GUI login with dialog.
     * 
     * Shows a login dialog and validates credentials.
     * 
     * @return true if login successful, false otherwise
     */
    private boolean performGUILogin() {
        // Create and show login dialog
        ui.LoginDialog loginDialog = new ui.LoginDialog(authService);
        loginDialog.setVisible(true);
        
        // Wait for dialog to close and check result
        return loginDialog.isLoginSuccessful();
    }
    
    /**
     * Read password from console (without echoing).
     * 
     * Uses System.console() if available for secure input,
     * otherwise falls back to Scanner.
     * 
     * @return Password entered by user
     */
    private String readPassword() {
        java.io.Console console = System.console();
        if (console != null) {
            // Secure password input (no echo)
            char[] passwordChars = console.readPassword();
            return new String(passwordChars);
        } else {
            // Fallback for IDEs without console support
            return scanner.nextLine();
        }
    }
    
    
    // ============================================================================== //
    //                          SESSION RETRIEVAL MANAGEMENT                          //
    // ============================================================================== //
    
    /**
     * Start data collection threads for all known Agents
     * 
     * For each Agent in database:
     * ### Step 1: Get Agent's IP address
     * ### Step 2: Create dedicated thread for that Agent
     * ### Step 3: Thread continuously requests session data
     * 
     * Each thread polls its Agent at configured intervals
     */
    private void startSessionRetrieving() {
        // Get all known Agents from database (MAC -> IP mapping)
        Map<String, String> ipMacMap = computerManager.getAllComputerIpMap();
        
        // Create one thread per Agent
        for (String mac : ipMacMap.keySet()) {
            String ip = ipMacMap.get(mac);
            startSessionRequestThread(mac, ip);
        }
    }

    /**
     * Start a SessionRequest thread for a single Agent
     * Called when a new Agent is discovered or at startup for known Agents
     * 
     * @param macAddress Agent's MAC address
     * @param ipAddress Agent's IP address
     */
    private void startSessionRequestThread(String macAddress, String ipAddress) {
        // Check if thread already exists for this MAC
        for (SessionRetriever.SessionRequest thread : sessionRequestThreads) {
            if (thread.macAddress.equals(macAddress)) {
                System.out.println("SessionRequest thread already running for Agent: " + macAddress);
                return;
            }
        }
        
        // Create and start new SessionRequest thread
        SessionRetriever.SessionRequest requestThread = 
            new SessionRetriever.SessionRequest(ipAddress, appConfig.getAgentUdpPort(), macAddress, 
                                               sessionRetriever, appConfig);
        requestThread.start();
        sessionRequestThreads.add(requestThread);
        System.out.println("Started SessionRequest thread for Agent: " + macAddress + " at " + ipAddress);
    }

    /**
     * Implementation of AgentDiscoveryListener interface
     * Called by SessionRetriever when a new Agent's computer info is saved
     * 
     * This callback allows automatic thread creation without SessionRetriever knowing about ManagerMain
     * Follows Dependency Inversion Principle and Observer Pattern
     * 
     * @param macAddress MAC address of newly discovered Agent
     * @param ipAddress IP address of newly discovered Agent
     */
    @Override
    public void onAgentDiscovered(String macAddress, String ipAddress) {
        System.out.println("=== New Agent Discovered ===");
        System.out.println("MAC: " + macAddress);
        System.out.println("IP: " + ipAddress);
        System.out.println("Starting session collection...");
        startSessionRequestThread(macAddress, ipAddress);
    }
}
