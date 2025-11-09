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
 * - Agent now sends data directly to Manager without using local database
 * 
 * Responsibilities:
 * 1. Initialize all application components with proper dependency injection
 * 2. Start scheduled tasks for data collection
 * 3. Start network message service for communication with Manager
 * 4. Handle graceful shutdown
 */
public class AgentMain {

    // ### All dependencies are injected through constructor (no static state)
    private final SessionRetriever sessionRetriever;
    private final ComputerRetriever computerRetriever;
    private final AppConfig appConfig;
    private final ProtocolManager protocolManager;
    private final ScanningMonitor scanningMonitor;
    private final ComputerSendingMonitor computerSendingMonitor;
    private final SessionSendingMonitor sessionSendingMonitor;
    private final NetworkMessageService networkMessageService;

    /**
     * Constructor - Initializes all dependencies in correct order
     * 
     * Dependency Injection Flow:
     * ### Step 1: Load application configuration
     * ### Step 2: Initialize data retrievers (collect system info on-demand)
     * ### Step 3: Initialize protocol handlers and monitors
     * ### Step 4: Initialize network communication service
     */
    public AgentMain() {
        // ### Step 1: Load application configuration
        this.appConfig = new AppConfig();
        
        // ### Step 2: Initialize data retrievers (no database, just in-memory)
        // ComputerRetriever collects hardware/system information
        this.computerRetriever = new ComputerRetriever();
        
        // SessionRetriever collects CPU/RAM usage and running processes
        this.sessionRetriever = new SessionRetriever(computerRetriever);
        
        // ### Step 3: Initialize protocol handlers and monitors
        // ProtocolManager defines message format for Agent-Manager communication
        this.protocolManager = new ProtocolManager();
        
        // Monitors handle specific types of incoming messages
        this.scanningMonitor = new ScanningMonitor(protocolManager, computerRetriever);
        this.computerSendingMonitor = new ComputerSendingMonitor(protocolManager, computerRetriever);
        this.sessionSendingMonitor = new SessionSendingMonitor(protocolManager, sessionRetriever);
        
        // ### Step 4: Initialize network service for UDP communication
        this.networkMessageService = new NetworkMessageService(appConfig, scanningMonitor, 
                                                               computerSendingMonitor, sessionSendingMonitor);
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
     * ### Step 1: Collect initial computer info (once at startup)
     * ### Step 2: Start network service (listen for Manager requests)
     * ### Step 3: Gracefully shutdown when interrupted
     */
    private void run(boolean gui) {
        if (gui) {
            new AgentWindow(() -> {
                startServices();
            }, () -> {
                networkMessageService.stopNow();
            }, sessionRetriever, computerRetriever, appConfig);
        } else {
            startServices();

            shutdown();
        }
    }

    /**
     * Gracefully shutdown all services
     * ### Step 1: Wait for network service thread to complete
     * ### Step 2: Close network socket
     */
    private void shutdown() {
        try {
            networkMessageService.join();  // Wait for network thread to finish
            networkMessageService.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Start all background services
     * 
     * ### Step 1: Collect computer info IMMEDIATELY (required for Manager requests)
     *   - Collects hardware specs, OS info, CPU details
     *   - Stored in-memory for fast access
     *   - Computer info rarely changes, so collect once at startup
     * 
     * ### Step 2: Start UDP network service
     *   - Listens for messages from Manager
     *   - Responds to data requests
     *   - Session data will be collected ON-DEMAND when Manager requests it
     */
    private void startServices() {
        // ### Step 1: Collect computer info IMMEDIATELY (once at startup)
        // computerRetriever.run();

        // ### Step 2: Start network communication service
        try {
            networkMessageService.open();   // Open UDP socket
            networkMessageService.start();  // Start message listening thread
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
