package config;

/**
 * AppConfig - Application configuration
 * 
 * Configuration is loaded from ~/PBL4DATA/config.json
 * Can be modified at runtime and saved back to file
 */
public class AppConfig {
    // Database URLs
    private String databaseUrl;
    private String authDatabaseUrl;
    private String emailDatabaseUrl;
    
    // Network ports - Agent side (where Agent listens)
    private int agentScanComputerPort;  // Agent port for HELLO and GET_COMPUTER_INFO (default: 5000)
    private int agentSessionPort;       // Agent port for GET_SESSION and processes (default: 5001)
    
    // Legacy Agent ports (deprecated, kept for backward compatibility)
    @Deprecated
    private int agentUdpPort;  // Maps to agentScanComputerPort
    private int agentTcpPort;
    
    // Network ports - Manager side (where Manager listens)
    private int managerUdpPort;  // Legacy, maps to managerScanPort
    private int managerScanPort;      // Manager port for scan/HELLO responses (default: 6000)
    private int managerSessionPort;   // Manager port for session/computer/process data (default: 6001)
    private int managerTcpPort;
    private int remoteCommandPort;
    private int externalScanPort;
    
    // Session settings
    private int sessionRetrievingDelayMs;
    
    // Alert thresholds
    private double cpuThresholdPercent;  // CPU usage threshold (default: 90.0)
    private double ramThresholdPercent;  // RAM usage threshold (default: 90.0)
    
    // User preferences
    private String language; // "en" or "vi"
    
    // ============================================================================
    //                              GETTERS
    // ============================================================================
    
    public String getDatabaseUrl() {
        return databaseUrl;
    }
    
    public String getAuthDatabaseUrl() {
        return authDatabaseUrl;
    }
    
    public String getEmailDatabaseUrl() {
        return emailDatabaseUrl;
    }
    
    public int getAgentScanComputerPort() {
        return agentScanComputerPort;
    }
    
    public int getAgentSessionPort() {
        return agentSessionPort;
    }
    
    @Deprecated
    public int getAgentUdpPort() {
        return agentUdpPort != 0 ? agentUdpPort : agentScanComputerPort;
    }
    
    public int getAgentTcpPort() {
        return agentTcpPort;
    }
    
    public int getManagerUdpPort() {
        return managerUdpPort != 0 ? managerUdpPort : managerScanPort;
    }
    
    public int getManagerScanPort() {
        return managerScanPort;
    }
    
    public int getManagerSessionPort() {
        return managerSessionPort;
    }
    
    public int getManagerTcpPort() {
        return managerTcpPort;
    }
    
    public int getRemoteCommandPort() {
        return remoteCommandPort;
    }
    
    public int getExternalScanPort() {
        return externalScanPort;
    }
    
    public int getSessionRetrievingDelayMs() {
        return sessionRetrievingDelayMs;
    }
    
    public double getCpuThresholdPercent() {
        return cpuThresholdPercent;
    }
    
    public double getRamThresholdPercent() {
        return ramThresholdPercent;
    }
    
    public String getLanguage() {
        return language;
    }
    
    // ============================================================================
    //                              SETTERS
    // ============================================================================
    
    public void setDatabaseUrl(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }
    
    public void setAuthDatabaseUrl(String authDatabaseUrl) {
        this.authDatabaseUrl = authDatabaseUrl;
    }
    
    public void setEmailDatabaseUrl(String emailDatabaseUrl) {
        this.emailDatabaseUrl = emailDatabaseUrl;
    }
    
    public void setAgentScanComputerPort(int agentScanComputerPort) {
        this.agentScanComputerPort = agentScanComputerPort;
    }
    
    public void setAgentSessionPort(int agentSessionPort) {
        this.agentSessionPort = agentSessionPort;
    }
    
    @Deprecated
    public void setAgentUdpPort(int agentUdpPort) {
        this.agentUdpPort = agentUdpPort;
        this.agentScanComputerPort = agentUdpPort;  // Sync with new port
    }
    
    public void setAgentTcpPort(int agentTcpPort) {
        this.agentTcpPort = agentTcpPort;
    }
    
    public void setManagerUdpPort(int managerUdpPort) {
        this.managerUdpPort = managerUdpPort;
    }
    
    public void setManagerScanPort(int managerScanPort) {
        this.managerScanPort = managerScanPort;
    }
    
    public void setManagerSessionPort(int managerSessionPort) {
        this.managerSessionPort = managerSessionPort;
    }
    
    public void setManagerTcpPort(int managerTcpPort) {
        this.managerTcpPort = managerTcpPort;
    }
    
    public void setRemoteCommandPort(int remoteCommandPort) {
        this.remoteCommandPort = remoteCommandPort;
    }
    
    public void setExternalScanPort(int externalScanPort) {
        this.externalScanPort = externalScanPort;
    }
    
    public void setSessionRetrievingDelayMs(int sessionRetrievingDelayMs) {
        this.sessionRetrievingDelayMs = sessionRetrievingDelayMs;
    }
    
    public void setCpuThresholdPercent(double cpuThresholdPercent) {
        this.cpuThresholdPercent = cpuThresholdPercent;
    }
    
    public void setRamThresholdPercent(double ramThresholdPercent) {
        this.ramThresholdPercent = ramThresholdPercent;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    // End of AppConfig
}