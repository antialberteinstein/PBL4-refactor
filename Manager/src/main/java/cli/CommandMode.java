package cli;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import config.AppConfig;
import database.ComputerManager;
import database.ProcessManager;
import database.SessionManager;
import service.HostScanner;
import util.Messages;

/**
 * CommandMode - Execute single command without entering interactive mode
 * 
 * This mode allows running Manager commands directly from the command line
 * without starting an interactive CLI session.
 * 
 * Command Types:
 * 1. SCAN command: Connects to ExternalScanServer of a running interactive/GUI Manager instance
 * 2. Other commands: Directly queries the database and displays results
 * 
 * Usage Examples:
 *   java ManagerMain -c scan                    - Trigger scan on running Manager
 *   java ManagerMain -c "list agents"           - Show all agents
 *   java ManagerMain -c "list sessions"         - Show all sessions
 *   java ManagerMain -c "list processes"        - Show all processes
 *   java ManagerMain --command "show agents"    - Same as list agents
 * 
 * Requirements:
 * - For SCAN command: An interactive or GUI Manager instance MUST be running
 * - For other commands: Only database access is required (no Manager instance needed)
 * 
 * Architecture:
 * - Uses existing CommandExecutor for command parsing and execution
 * - Implements TCP client to communicate with ExternalScanServer for scan requests
 * - Direct database access for query commands
 */
public class CommandMode {
    
    // ============================================================================== //
    //                                 SECTION: CONSTANTS                             //
    // ============================================================================== //
    
    /** Timeout for TCP connection to ExternalScanServer (milliseconds) */
    private static final int TCP_TIMEOUT_MS = 5000;
    
    /** Localhost address for connecting to ExternalScanServer */
    private static final String LOCALHOST = "localhost";
    
    
    // ============================================================================== //
    //                                SECTION: DEPENDENCIES                           //
    // ============================================================================== //
    
    private final AppConfig appConfig;
    private final CommandExecutor executor;
    
    
    // ============================================================================== //
    //                                SECTION: CONSTRUCTOR                            //
    // ============================================================================== //
    
    /**
     * Constructor - Initialize command mode with required dependencies
     * 
     * ### Step 1: Store dependencies
     * ### Step 2: Create CommandExecutor with dummy CliContext (not used in command mode)
     * 
     * @param appConfig Configuration (for TCP port)
     * @param computerManager Database manager for agents
     * @param sessionManager Database manager for sessions
     * @param processManager Database manager for processes
     * @param hostScanner Scanner (not used in command mode, but required by CommandExecutor)
     */
    public CommandMode(AppConfig appConfig, ComputerManager computerManager,
                      SessionManager sessionManager, ProcessManager processManager,
                      HostScanner hostScanner) {
        // ### Step 1: Store dependencies
        this.appConfig = appConfig;
        
        // ### Step 2: Create CommandExecutor
        // Note: CliContext is created but context-based commands won't work in command mode
        // Only absolute commands (list agents, list sessions, etc.) are supported
        CliContext context = new CliContext();
        this.executor = new CommandExecutor(context, computerManager, sessionManager, 
                                           processManager, hostScanner);
    }
    
    
    // ============================================================================== //
    //                           SECTION: PUBLIC INTERFACE                            //
    // ============================================================================== //
    
    /**
     * Execute a single command and exit
     * 
     * Workflow:
     * ### Step 1: Normalize command (trim whitespace)
     * ### Step 2: Check if command is SCAN
     * ### Step 3a: If SCAN - Connect to ExternalScanServer via TCP
     * ### Step 3b: If other - Execute via CommandExecutor (queries database)
     * ### Step 4: Print result and exit
     * 
     * @param command Command to execute (e.g., "scan", "list agents", "show sessions")
     * @return Exit code (0 = success, 1 = error)
     */
    public int executeCommand(String command) {
        // ### Step 1: Validate command
        if (command == null || command.trim().isEmpty()) {
            System.err.println(Messages.get("cli.cmd.error.empty"));
            showCommandUsage();
            return 1;
        }
        
        String normalizedCommand = command.trim();
        String commandLower = normalizedCommand.toLowerCase();
        
        // ### Step 2: Check if command is SCAN
        if (commandLower.equals("scan")) {
            // ### Step 3a: Execute SCAN via TCP
            return executeScanCommand();
        } else {
            // ### Step 3b: Execute other commands via database
            return executeQueryCommand(normalizedCommand);
        }
    }
    
    
    // ============================================================================== //
    //                           SECTION: SCAN COMMAND (TCP)                          //
    // ============================================================================== //
    
    /**
     * Execute SCAN command by connecting to ExternalScanServer
     * 
     * Workflow:
     * ### Step 1: Create TCP connection to localhost:EXTERNAL_SCAN_PORT
     * ### Step 2: Send "SCAN\n" command
     * ### Step 3: Read response ("OK\n" or "ERROR: ...\n")
     * ### Step 4: Print result
     * ### Step 5: Close connection
     * 
     * @return Exit code (0 = success, 1 = error)
     */
    private int executeScanCommand() {
        Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        
        try {
            // ### Step 1: Create TCP connection with timeout
            socket = new Socket(LOCALHOST, appConfig.getExternalScanPort());
            socket.setSoTimeout(TCP_TIMEOUT_MS);
            
            // ### Step 2: Setup I/O streams
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // ### Step 3: Send SCAN command
            out.println("SCAN");
            
            // ### Step 4: Read response
            String response = in.readLine();
            
            if (response == null) {
                System.err.println(Messages.get("cli.cmd.error.no.response"));
                return 1;
            }
            
            // ### Step 5: Parse and display response
            if (response.startsWith("OK")) {
                System.out.println(Messages.get("cli.cmd.scan.success"));
                System.out.println(Messages.get("cli.cmd.scan.progress"));
                return 0;
            } else if (response.startsWith("ERROR")) {
                System.err.println(Messages.get("cli.cmd.scan.failed", response.substring(7)));
                return 1;
            } else {
                System.err.println(Messages.get("cli.cmd.scan.unexpected", response));
                return 1;
            }
            
        } catch (java.net.ConnectException e) {
            // Connection refused - Manager not running
            System.err.println(Messages.get("cli.cmd.error.cannot.connect"));
            System.err.println(Messages.get("cli.cmd.error.make.sure"));
            System.err.println(Messages.get("cli.cmd.error.start.manager"));
            return 1;
            
        } catch (java.net.SocketTimeoutException e) {
            // Timeout - Manager not responding
            System.err.println(Messages.get("cli.cmd.error.timeout"));
            System.err.println(Messages.get("cli.cmd.error.not.responding", appConfig.getExternalScanPort()));
            return 1;
            
        } catch (Exception e) {
            // Other errors
            System.err.println(Messages.get("cli.cmd.error.scan", e.getMessage()));
            return 1;
            
        } finally {
            // ### Step 6: Cleanup resources
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (socket != null) socket.close();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
    
    
    // ============================================================================== //
    //                         SECTION: QUERY COMMANDS (DATABASE)                     //
    // ============================================================================== //
    
    /**
     * Execute query command via CommandExecutor (direct database access)
     * 
     * Workflow:
     * ### Step 1: Execute command via CommandExecutor
     * ### Step 2: Print result
     * 
     * Supported commands:
     * - list agents / show agents
     * - list sessions / show sessions
     * - list processes / show processes
     * - help / ?
     * - status
     * 
     * Note: Context-based commands (use, head, tail) are NOT supported in command mode
     * 
     * @param command Command to execute
     * @return Exit code (0 = success, 1 = error)
     */
    private int executeQueryCommand(String command) {
        try {
            // ### Step 1: Execute command
            String result = executor.execute(command);
            
            // ### Step 2: Check if result indicates error
            if (result.startsWith("Error:") || result.startsWith("✗")) {
                System.err.println(result);
                return 1;
            }
            
            // ### Step 3: Print result
            System.out.println(result);
            return 0;
            
        } catch (Exception e) {
            System.err.println(Messages.get("cli.cmd.error.command", e.getMessage()));
            return 1;
        }
    }
    
    
    // ============================================================================== //
    //                              SECTION: HELP & USAGE                             //
    // ============================================================================== //
    
    /**
     * Show command usage examples
     */
    private void showCommandUsage() {
        System.out.println();
        System.out.println(Messages.get("cli.cmd.usage.title"));
        System.out.println(Messages.get("cli.cmd.usage.format1"));
        System.out.println(Messages.get("cli.cmd.usage.format2"));
        System.out.println();
        System.out.println(Messages.get("cli.cmd.commands.title"));
        System.out.println(Messages.get("cli.cmd.commands.scan"));
        System.out.println(Messages.get("cli.cmd.commands.list.agents"));
        System.out.println(Messages.get("cli.cmd.commands.list.sessions"));
        System.out.println(Messages.get("cli.cmd.commands.list.processes"));
        System.out.println(Messages.get("cli.cmd.commands.help"));
        System.out.println(Messages.get("cli.cmd.commands.status"));
        System.out.println();
        System.out.println(Messages.get("cli.cmd.examples.title"));
        System.out.println(Messages.get("cli.cmd.examples.scan"));
        System.out.println(Messages.get("cli.cmd.examples.list.agents"));
        System.out.println(Messages.get("cli.cmd.examples.list.sessions"));
        System.out.println();
        System.out.println(Messages.get("cli.cmd.note.title"));
        System.out.println(Messages.get("cli.cmd.note.scan.requires"));
        System.out.println(Messages.get("cli.cmd.note.other.commands"));
        System.out.println();
    }
}
