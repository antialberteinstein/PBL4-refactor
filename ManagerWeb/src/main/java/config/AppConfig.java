package config;

/**
 * AppConfig - Application configuration for ManagerWeb
 * 
 * Configuration is loaded from ~/PBL4DATA/config.json
 * Shared with Manager GUI application
 */
public class AppConfig {
    // Database URLs
    private String databaseUrl;
    private String authDatabaseUrl;
    
    // Network ports
    private int agentUdpPort;
    private int agentTcpPort;
    private int managerUdpPort;
    private int managerTcpPort;
    private int externalScanPort;
    
    // Session settings
    private int sessionRetrievingDelayMs;
    
    // User preferences
    private String language; // "en" or "vi"
    
    // Resource monitoring thresholds
    private double cpuThresholdPercent = 90.0;
    private double ramThresholdPercent = 90.0;
    
    // ============================================================================
    //                              GETTERS
    // ============================================================================
    
    public String getDatabaseUrl() {
        return databaseUrl;
    }
    
    public String getAuthDatabaseUrl() {
        return authDatabaseUrl;
    }
    
    public int getAgentUdpPort() {
        return agentUdpPort;
    }
    
    public int getAgentTcpPort() {
        return agentTcpPort;
    }
    
    public int getManagerUdpPort() {
        return managerUdpPort;
    }
    
    public int getManagerTcpPort() {
        return managerTcpPort;
    }
    
    public int getExternalScanPort() {
        return externalScanPort;
    }
    
    public int getSessionRetrievingDelayMs() {
        return sessionRetrievingDelayMs;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public double getCpuThresholdPercent() {
        return cpuThresholdPercent;
    }
    
    public double getRamThresholdPercent() {
        return ramThresholdPercent;
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
    
    public void setAgentUdpPort(int agentUdpPort) {
        this.agentUdpPort = agentUdpPort;
    }
    
    public void setAgentTcpPort(int agentTcpPort) {
        this.agentTcpPort = agentTcpPort;
    }
    
    public void setManagerUdpPort(int managerUdpPort) {
        this.managerUdpPort = managerUdpPort;
    }
    
    public void setManagerTcpPort(int managerTcpPort) {
        this.managerTcpPort = managerTcpPort;
    }
    
    public void setExternalScanPort(int externalScanPort) {
        this.externalScanPort = externalScanPort;
    }
    
    public void setSessionRetrievingDelayMs(int sessionRetrievingDelayMs) {
        this.sessionRetrievingDelayMs = sessionRetrievingDelayMs;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public void setCpuThresholdPercent(double cpuThresholdPercent) {
        this.cpuThresholdPercent = cpuThresholdPercent;
    }
    
    public void setRamThresholdPercent(double ramThresholdPercent) {
        this.ramThresholdPercent = ramThresholdPercent;
    }
    
    // ============================================================================
    // ============================================================================
    //                           LEGACY CONSTANTS
    // ============================================================================
    // Deprecated legacy accessor methods were removed to simplify the API.
    // If backward compatibility with external tooling is required, re-add them.
}
