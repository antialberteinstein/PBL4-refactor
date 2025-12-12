package service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import config.AppConfig;
import util.Logger;

/**
 * ExternalScanServer - TCP Server for receiving external scan requests
 * 
 * This server listens on a dedicated TCP port and accepts scan commands from external clients.
 * It provides a simple interface for remote systems to trigger network scans without direct
 * access to the Manager's CLI or GUI.
 * 
 * Protocol:
 * - Client sends: "SCAN\n"
 * - Server responds: "OK\n" if scan is triggered
 * - Server responds: "ERROR: <reason>\n" if scan cannot be triggered
 * 
 * Security Note:
 * This server accepts connections from any client. In production, consider implementing:
 * - IP whitelist/blacklist
 * - Authentication mechanism
 * - Rate limiting
 * 
 * Thread Safety:
 * - This server runs in its own thread
 * - HostScanner.scan() is thread-safe
 */
public class ExternalScanServer extends Thread {
    
    // ============================================================================== //
    //                                 SECTION: CONSTANTS                             //
    // ============================================================================== //
    
    private static final String COMPONENT = "ExternalScanServer";
    
    /** Command to trigger a network scan */
    private static final String SCAN_COMMAND = "SCAN";
    
    /** Response when scan is successfully triggered */
    private static final String RESPONSE_OK = "OK";
    
    /** Response prefix for error messages */
    private static final String RESPONSE_ERROR = "ERROR";
    
    /** Socket timeout for accept() to allow checking shutdown flag */
    private static final int ACCEPT_TIMEOUT_MS = 1000;
    
    
    // ============================================================================== //
    //                                SECTION: DEPENDENCIES                           //
    // ============================================================================== //
    
    private final AppConfig appConfig;
    private final HostScanner hostScanner;
    private final RemoteCommandClient remoteCommandClient;
    private final database.ComputerManager computerManager;
    
    
    // ============================================================================== //
    //                               SECTION: STATE VARIABLES                         //
    // ============================================================================== //
    
    private ServerSocket serverSocket;
    private volatile boolean running;
    
    
    // ============================================================================== //
    //                                SECTION: CONSTRUCTOR                            //
    // ============================================================================== //
    
    /**
     * Constructor - Initialize external scan server
     * 
     * @param appConfig Configuration containing the TCP port
     * @param hostScanner HostScanner instance to trigger scans
     * @param remoteCommandClient Client for sending commands to Agents
     * @param computerManager Manager for looking up Agent IP addresses
     */
    public ExternalScanServer(
        AppConfig appConfig,
        HostScanner hostScanner,
        RemoteCommandClient remoteCommandClient,
        database.ComputerManager computerManager
    ) {
        this.appConfig = appConfig;
        this.hostScanner = hostScanner;
        this.remoteCommandClient = remoteCommandClient;
        this.computerManager = computerManager;
        this.running = false;
        
        // Set thread name for easier debugging
        this.setName("ExternalScanServer-Thread");
    }
    
    
    // ============================================================================== //
    //                               SECTION: SERVER LIFECYCLE                        //
    // ============================================================================== //
    
    /**
     * Start the TCP server
     * 
     * Workflow:
     * ### 1. Create ServerSocket and bind to configured port
     * ### 2. Set socket timeout to allow periodic shutdown checks
     * ### 3. Enter main server loop
     */
    @Override
    public void run() {
        try {
            // ### 1. Create ServerSocket and bind to port
            serverSocket = new ServerSocket(appConfig.getExternalScanPort());
            
            // ### 2. Set socket timeout for responsive shutdown
            serverSocket.setSoTimeout(ACCEPT_TIMEOUT_MS);
            
            running = true;
            Logger.info(COMPONENT, "External Scan Server started on port " + appConfig.getExternalScanPort());
            
            // ### 3. Main server loop
            while (running) {
                try {
                    // Wait for incoming connection (with timeout)
                    Socket clientSocket = serverSocket.accept();
                    
                    // Handle client in separate thread to avoid blocking
                    handleClient(clientSocket);
                    
                } catch (SocketTimeoutException e) {
                    // Timeout is expected - allows checking 'running' flag
                    // No action needed, continue loop
                }
            }
            
        } catch (IOException e) {
            if (running) {
                // Only log error if we're still supposed to be running
                // (ignore errors during shutdown)
                Logger.error(COMPONENT, "Server error: " + e.getMessage());
            }
        } finally {
            cleanup();
        }
    }
    
    /**
     * Shutdown the server gracefully
     * 
     * Workflow:
     * ### 1. Set running flag to false
     * ### 2. Close server socket (will interrupt accept())
     * ### 3. Wait for server thread to finish
     */
    public void shutdown() {
        Logger.info(COMPONENT, "Shutting down External Scan Server...");
        
        // ### 1. Signal server to stop
        running = false;
        
        // ### 2. Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Logger.error(COMPONENT, "Error closing server socket: " + e.getMessage());
        }
        
        // ### 3. Wait for thread to finish (with timeout)
        try {
            this.join(2000); // Wait max 2 seconds
        } catch (InterruptedException e) {
            Logger.error(COMPONENT, "Interrupted while waiting for server thread to finish");
            Thread.currentThread().interrupt();
        }
        
        Logger.info(COMPONENT, "External Scan Server stopped");
    }
    
    /**
     * Cleanup resources
     */
    private void cleanup() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Logger.error(COMPONENT, "Error during cleanup: " + e.getMessage());
        }
    }
    
    
    // ============================================================================== //
    //                              SECTION: CLIENT HANDLING                          //
    // ============================================================================== //
    
    /**
     * Handle a client connection
     * 
     * This method runs in a separate thread for each client to avoid blocking
     * the main server loop. It processes one command per connection.
     * 
     * Workflow:
     * ### 1. Create input/output streams
     * ### 2. Read command from client
     * ### 3. Process command
     * ### 4. Send response
     * ### 5. Close connection
     * 
     * @param clientSocket The connected client socket
     */
    private void handleClient(Socket clientSocket) {
        // Run in separate thread to avoid blocking main server loop
        new Thread(() -> {
            String clientAddress = clientSocket.getInetAddress().getHostAddress();
            Logger.debug(COMPONENT, "Client connected from: " + clientAddress);
            
            try (
                // ### 1. Create streams
                // Auto-close streams when done
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream())
                );
                PrintWriter out = new PrintWriter(
                    clientSocket.getOutputStream(), 
                    true // Auto-flush
                )
            ) {
                // ### 2. Read command from client
                String command = in.readLine();
                
                if (command == null) {
                    // Client closed connection before sending command
                    Logger.debug(COMPONENT, "Client disconnected before sending command");
                    return;
                }
                
                command = command.trim();
                Logger.debug(COMPONENT, "Received command from " + clientAddress + ": " + command);
                
                // ### 3. Process command
                String response = processCommand(command);
                
                // ### 4. Send response
                out.println(response);
                Logger.debug(COMPONENT, "Sent response to " + clientAddress + ": " + response);
                
            } catch (IOException e) {
                Logger.error(COMPONENT, "Error handling client " + clientAddress + ": " + e.getMessage());
            } finally {
                // ### 5. Close connection
                try {
                    clientSocket.close();
                    Logger.debug(COMPONENT, "Closed connection from " + clientAddress);
                } catch (IOException e) {
                    Logger.error(COMPONENT, "Error closing client socket: " + e.getMessage());
                }
            }
        }).start();
    }
    
    /**
     * Process a command from client
     * 
     * Supported commands:
     * - SCAN: Trigger network scan
     * - KILL_PROCESS|MAC|PID: Kill process on Agent
     * - SHUTDOWN|MAC|DELAY: Shutdown Agent
     * - SEND_MESSAGE|MAC|MESSAGE: Send message to Agent
     * 
     * @param command Command string (trimmed, case-sensitive for parameters)
     * @return Response string to send back to client
     */
    private String processCommand(String command) {
        // ### 1. Check if command is SCAN (case-insensitive)
        if (SCAN_COMMAND.equalsIgnoreCase(command)) {
            return handleScanCommand();
        }
        
        // ### 2. Check if command has parameters (separated by |)
        String[] parts = command.split("\\|");
        if (parts.length < 2) {
            return RESPONSE_ERROR + ": Invalid command format. Use 'SCAN' or 'COMMAND|MAC|PARAMS'.";
        }
        
        String commandType = parts[0].trim().toUpperCase(); // Only uppercase command type
        String macAddress = parts[1].trim(); // Keep MAC address as-is
        
        // ### 3. Handle different command types
        switch (commandType) {
            case "KILL_PROCESS":
                if (parts.length < 3) {
                    return RESPONSE_ERROR + ": KILL_PROCESS requires PID. Format: KILL_PROCESS|MAC|PID";
                }
                return handleKillProcessCommand(macAddress, parts[2].trim());
                
            case "SHUTDOWN":
                String delayStr = parts.length >= 3 ? parts[2].trim() : "60";
                return handleShutdownCommand(macAddress, delayStr);
                
            case "SEND_MESSAGE":
                if (parts.length < 3) {
                    return RESPONSE_ERROR + ": SEND_MESSAGE requires message. Format: SEND_MESSAGE|MAC|MESSAGE";
                }
                // Rejoin remaining parts as message (in case message contains |)
                StringBuilder message = new StringBuilder(parts[2]);
                for (int i = 3; i < parts.length; i++) {
                    message.append("|").append(parts[i]);
                }
                return handleSendMessageCommand(macAddress, message.toString());
                
            default:
                return RESPONSE_ERROR + ": Unknown command '" + commandType + "'. Use SCAN, KILL_PROCESS, SHUTDOWN, or SEND_MESSAGE.";
        }
    }
    
    /**
     * Handle the SCAN command
     * 
     * Triggers a network scan using the HostScanner.
     * 
     * @return Response string indicating success or failure
     */
    private String handleScanCommand() {
        try {
            // Trigger network scan
            Logger.info(COMPONENT, "Triggering network scan from external request");
            
            // Clear cache to force re-discovery of all agents
            // This fixes issues where deleted agents (on Web) were not re-discovered
            // because Manager still had them in cache.
            hostScanner.clearCache();
            
            hostScanner.scan();
            
            // Return success response
            return RESPONSE_OK;
            
        } catch (Exception e) {
            // Log error and return error response
            Logger.error(COMPONENT, "Error triggering scan: " + e.getMessage());
            return RESPONSE_ERROR + ": Failed to trigger scan - " + e.getMessage();
        }
    }
    
    /**
     * Handle KILL_PROCESS command
     * 
     * @param macAddress MAC address of target Agent
     * @param pidStr Process ID to kill
     * @return Response string
     */
    private String handleKillProcessCommand(String macAddress, String pidStr) {
        try {
            // Parse PID
            int pid = Integer.parseInt(pidStr);
            
            // Get Agent IP from MAC
            String agentIp = computerManager.getIpByMac(macAddress);
            if (agentIp == null) {
                Logger.error(COMPONENT, "Agent not found for MAC: " + macAddress);
                return RESPONSE_ERROR + ": Agent not found for MAC " + macAddress;
            }
            
            // Send kill command to Agent
            Logger.info(COMPONENT, "Sending KILL_PROCESS command to Agent " + macAddress + " (" + agentIp + ") for PID " + pid);
            
            try {
                remoteCommandClient.killProcess(agentIp, pid);
                return RESPONSE_OK + ": Process " + pid + " killed on Agent " + macAddress;
            } catch (Exception e) {
                return RESPONSE_ERROR + ": Failed to kill process " + pid + " on Agent " + macAddress + " - " + e.getMessage();
            }
            
        } catch (NumberFormatException e) {
            return RESPONSE_ERROR + ": Invalid PID '" + pidStr + "'. Must be a number.";
        } catch (Exception e) {
            Logger.error(COMPONENT, "Error handling KILL_PROCESS command: " + e.getMessage());
            return RESPONSE_ERROR + ": " + e.getMessage();
        }
    }
    
    /**
     * Handle SHUTDOWN command
     * 
     * @param macAddress MAC address of target Agent
     * @param delayStr Delay in seconds before shutdown
     * @return Response string
     */
    private String handleShutdownCommand(String macAddress, String delayStr) {
        try {
            // Parse delay
            int delay = Integer.parseInt(delayStr);
            
            // Get Agent IP from MAC
            String agentIp = computerManager.getIpByMac(macAddress);
            if (agentIp == null) {
                Logger.error(COMPONENT, "Agent not found for MAC: " + macAddress);
                return RESPONSE_ERROR + ": Agent not found for MAC " + macAddress;
            }
            
            // Send shutdown command to Agent
            Logger.info(COMPONENT, "Sending SHUTDOWN command to Agent " + macAddress + " (" + agentIp + ") with delay " + delay + "s");
            
            try {
                remoteCommandClient.shutdown(agentIp, delay);
                return RESPONSE_OK + ": Shutdown scheduled on Agent " + macAddress + " in " + delay + " seconds";
            } catch (Exception e) {
                return RESPONSE_ERROR + ": Failed to shutdown Agent " + macAddress + " - " + e.getMessage();
            }
            
        } catch (NumberFormatException e) {
            return RESPONSE_ERROR + ": Invalid delay '" + delayStr + "'. Must be a number.";
        } catch (Exception e) {
            Logger.error(COMPONENT, "Error handling SHUTDOWN command: " + e.getMessage());
            return RESPONSE_ERROR + ": " + e.getMessage();
        }
    }
    
    /**
     * Handle SEND_MESSAGE command
     * 
     * @param macAddress MAC address of target Agent
     * @param message Message to send
     * @return Response string
     */
    private String handleSendMessageCommand(String macAddress, String message) {
        try {
            // Get Agent IP from MAC
            String agentIp = computerManager.getIpByMac(macAddress);
            if (agentIp == null) {
                Logger.error(COMPONENT, "Agent not found for MAC: " + macAddress);
                return RESPONSE_ERROR + ": Agent not found for MAC " + macAddress;
            }
            
            // Send message command to Agent
            Logger.info(COMPONENT, "Sending message to Agent " + macAddress + " (" + agentIp + "): " + message);
            
            try {
                remoteCommandClient.sendMessage(agentIp, message);
                return RESPONSE_OK + ": Message sent to Agent " + macAddress;
            } catch (Exception e) {
                return RESPONSE_ERROR + ": Failed to send message to Agent " + macAddress + " - " + e.getMessage();
            }
            
        } catch (Exception e) {
            Logger.error(COMPONENT, "Error handling SEND_MESSAGE command: " + e.getMessage());
            return RESPONSE_ERROR + ": " + e.getMessage();
        }
    }
}
