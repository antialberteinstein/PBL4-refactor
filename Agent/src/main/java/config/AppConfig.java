package config;

public class AppConfig {

    // UDP ports for different monitors
    public static final int AGENT_SCAN_COMPUTER_PORT = 5000;  // For ComputerSendingMonitor
    public static final int AGENT_SESSION_PORT = 5001;        // For SessionSendingMonitor (session and process)

    // TCP port for remote commands
    public static final int AGENT_COMMAND_PORT = 4000;        // For RemoteCommandServer

    // Interval for chart/UI refresh in milliseconds
    public static final int CHART_REFRESH_MILLIS = 1000; // 1 second by default
    
}
