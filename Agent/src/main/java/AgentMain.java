import service.*;
import ui.AgentWindow;
import config.AppConfig;
import util.ProtocolManager;

/**
 * AgentMain - Entry point for Agent application
 * 
 * Architecture:
 * - Uses Dependency Injection pattern for loose coupling
 * - Acts as the composition root where all dependencies are wired together
 * - Manages application lifecycle (initialization, running, shutdown)
 * - Each monitor runs as independent thread with its own UDP socket
 * 
 * Responsibilities:
 * 1. Initialize all application components with proper dependency injection
 * 2. Start monitor threads for handling different message types
 * 3. Handle graceful shutdown
 */
public class AgentMain {

    // ### All dependencies are injected through constructor (no static state)
    private final SessionRetriever sessionRetriever;
    private final ComputerRetriever computerRetriever;
    private final AppConfig appConfig;
    private final ProtocolManager protocolManager;
    private final ComputerSendingMonitor computerSendingMonitor;
    private final SessionSendingMonitor sessionSendingMonitor;
    private final RemoteCommandServer remoteCommandServer;

    /**
     * Constructor - Initializes all dependencies in correct order
     * 
     * Dependency Injection Flow:
     * ### Step 1: Load application configuration
     * ### Step 2: Initialize data retrievers (collect system info on-demand)
     * ### Step 3: Initialize protocol handlers and monitors
     */
    public AgentMain() {
        // ### Step 1: Load application configuration
        this.appConfig = new AppConfig();
        
        // ### Step 2: Initialize data retrievers (no database, just in-memory)
        // ComputerRetriever collects hardware/system information
        this.computerRetriever = new ComputerRetriever();
        
        // SessionRetriever collects CPU/RAM usage and running processes
        this.sessionRetriever = new SessionRetriever(computerRetriever);
        
        // ### Step 3: Initialize protocol handlers and monitors (each as independent thread)
        // ProtocolManager defines message format for Agent-Manager communication
        this.protocolManager = new ProtocolManager();
        
        // Each monitor has its own UDP socket and runs in separate thread
        this.computerSendingMonitor = new ComputerSendingMonitor(protocolManager, computerRetriever, appConfig);
        this.sessionSendingMonitor = new SessionSendingMonitor(protocolManager, sessionRetriever, appConfig);
        
        // TCP server for remote commands (kill process, shutdown, send message/warning)
        this.remoteCommandServer = new RemoteCommandServer(appConfig, protocolManager);
    }

    /**
     * Application entry point
     * ### Step 1: Create AgentMain instance with all dependencies
     * ### Step 2: Start the application
     */
    public static void main(String args[]) {
        AgentMain agent = new AgentMain();

        boolean gui = false;
        if (args.length > 0 && args[0].equalsIgnoreCase("--gui")) {
            gui = true;
        }

        agent.run(gui);
    }

    /**
     * Main application run method
     * Orchestrates the startup sequence:
     * ### Step 1: Start all monitor threads
     * ### Step 2: Gracefully shutdown when interrupted
     */
    private void run(boolean gui) {
        if (gui) {
            AgentWindow window = new AgentWindow(() -> {
                startServices();
            }, () -> {
                stopServices();
            }, sessionRetriever, computerRetriever, appConfig);
            
            // Set notification listener for GUI mode
            remoteCommandServer.setNotificationListener(window);
        } else {
            // CLI mode - notification will be printed to console
            System.out.println("Agent running in CLI mode...");
            System.out.println("Remote commands will be displayed here.");
            
            startServices();

            shutdown();
        }
    }

    /**
     * Gracefully shutdown all services
     * ### Step 1: Wait for all monitor threads to complete
     */
    private void shutdown() {
        try {
            computerSendingMonitor.join();
            sessionSendingMonitor.join();
            remoteCommandServer.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Start all background services
     * 
     * ### Step 1: Open UDP sockets for each monitor
     * ### Step 2: Start monitor threads
     *   - ComputerSendingMonitor: handles HELLO_REQUEST and GET_COMPUTER_INFO_REQUEST (port 5000)
     *   - SessionSendingMonitor: handles GET_SESSION_REQUEST (port 5001)
     * ### Step 3: Start TCP server for remote commands (port 4000)
     */
    private void startServices() {
        try {
            // ### Step 1: Open UDP sockets
            computerSendingMonitor.open();
            sessionSendingMonitor.open();
            
            // ### Step 2: Start monitor threads
            computerSendingMonitor.start();
            sessionSendingMonitor.start();
            
            // ### Step 3: Start TCP server for remote commands
            remoteCommandServer.open();
            remoteCommandServer.start();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Stop all services
     */
    private void stopServices() {
        computerSendingMonitor.close();
        sessionSendingMonitor.close();
        remoteCommandServer.close();
    }
}
