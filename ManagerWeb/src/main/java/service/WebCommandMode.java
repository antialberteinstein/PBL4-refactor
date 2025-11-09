package service;

import config.AppConfig;
import model.entity.Computer;
import model.entity.Session;
import model.entity.Process;
import model.repository.ComputerRepository;
import model.repository.SessionRepository;
import model.repository.ProcessRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

// ============================================================================== //
//                                    CLASS: WebCommandMode                      //
// ============================================================================== //

/**
 * Web-based command execution service.
 * Similar to CLI CommandMode but designed for web requests.
 * 
 * - SCAN command: Connects to TCP server (localhost:8888) of running Manager
 * - Other commands: Query database directly
 * 
 * Uses Dependency Injection pattern for all dependencies.
 */
public class WebCommandMode {
    
    // ============================================================================== //
    //                                      CONSTANTS                                 //
    // ============================================================================== //
    
    private static final String LOCALHOST = "127.0.0.1";
    private static final int TCP_TIMEOUT_MS = 5000;
    
    // ============================================================================== //
    //                                    DEPENDENCIES                                //
    // ============================================================================== //
    
    private final AppConfig appConfig;
    private final ComputerRepository computerRepository;
    private final SessionRepository sessionRepository;
    private final ProcessRepository processRepository;
    
    // ============================================================================== //
    //                                   CONSTRUCTOR                                  //
    // ============================================================================== //
    
    /**
     * Constructor with Dependency Injection.
     * 
     * @param appConfig Configuration containing TCP port
     * @param computerRepository Repository for computer/agent queries
     * @param sessionRepository Repository for session queries
     * @param processRepository Repository for process queries
     */
    public WebCommandMode(
        AppConfig appConfig,
        ComputerRepository computerRepository,
        SessionRepository sessionRepository,
        ProcessRepository processRepository
    ) {
        this.appConfig = appConfig;
        this.computerRepository = computerRepository;
        this.sessionRepository = sessionRepository;
        this.processRepository = processRepository;
    }
    
    // ============================================================================== //
    //                                  SCAN OPERATION                                //
    // ============================================================================== //
    
    /**
     * Execute SCAN command by connecting to TCP server.
     * Requires a running Manager instance with ExternalScanServer.
     * 
     * @return Result object containing success status and message
     */
    public CommandResult executeScan() {
        Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        
        try {
            // 1. Establish TCP connection
            socket = new Socket(LOCALHOST, appConfig.getExternalScanPort());
            socket.setSoTimeout(TCP_TIMEOUT_MS);
            
            // 2. Setup I/O streams
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // 3. Send SCAN command
            out.println("SCAN");
            
            // 4. Read response
            String response = in.readLine();
            
            // 5. Process response
            if (response != null && response.startsWith("OK")) {
                return CommandResult.success("Scan request sent successfully");
            } else {
                return CommandResult.error("Scan request failed: " + response);
            }
            
        } catch (java.net.ConnectException e) {
            // Manager is not running
            return CommandResult.error("Cannot connect to Manager. Make sure Manager instance is running.");
            
        } catch (java.net.SocketTimeoutException e) {
            // Connection timeout
            return CommandResult.error("Connection timeout. Manager may be busy.");
            
        } catch (Exception e) {
            // Other errors
            return CommandResult.error("Scan failed: " + e.getMessage());
            
        } finally {
            // 6. Cleanup resources
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
    
    // ============================================================================== //
    //                                DATABASE QUERIES                                //
    // ============================================================================== //
    
    /**
     * Get all computers/agents from database.
     * 
     * @return List of all computers
     */
    public List<Computer> getAllComputers() {
        return computerRepository.getAllComputers();
    }
    
    /**
     * Get computer by MAC address.
     * 
     * @param macAddress MAC address of the computer
     * @return Computer object or null if not found
     */
    public Computer getComputerByMac(String macAddress) {
        return computerRepository.getComputerByMac(macAddress);
    }
    
    /**
     * Get all sessions for a specific computer.
     * 
     * @param macAddress MAC address of the computer
     * @return List of sessions (ordered by timestamp ASC for charts)
     */
    public List<Session> getSessionsByMac(String macAddress) {
        // -1 means get all sessions
        return sessionRepository.getSessionsByMac(macAddress, -1);
    }
    
    /**
     * Get recent sessions for a specific computer with limit.
     * 
     * @param macAddress MAC address of the computer
     * @param limit Maximum number of sessions to return
     * @return List of recent sessions (ordered by timestamp ASC for charts)
     */
    public List<Session> getRecentSessions(String macAddress, int limit) {
        return sessionRepository.getSessionsByMac(macAddress, limit);
    }
    
    /**
     * Get processes for a specific session.
     * 
     * @param sessionId Session ID
     * @return List of processes
     */
    public List<Process> getProcessesBySession(long sessionId) {
        return processRepository.getProcessesBySessionId((int) sessionId);
    }
    
    /**
     * Get latest session for a computer.
     * Used for real-time data display.
     * 
     * @param macAddress MAC address of the computer
     * @return Latest session or null if not found
     */
    public Session getLatestSession(String macAddress) {
        List<Session> sessions = sessionRepository.getSessionsByMac(macAddress, 1);
        return sessions.isEmpty() ? null : sessions.get(0);
    }
    
    // ============================================================================== //
    //                                 RESULT CLASS                                   //
    // ============================================================================== //
    
    /**
     * Result object for command execution.
     * Encapsulates success status and message.
     */
    public static class CommandResult {
        private final boolean success;
        private final String message;
        
        private CommandResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public static CommandResult success(String message) {
            return new CommandResult(true, message);
        }
        
        public static CommandResult error(String message) {
            return new CommandResult(false, message);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
