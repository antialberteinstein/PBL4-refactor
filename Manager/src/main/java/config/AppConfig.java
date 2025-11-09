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
    
    // Network ports
    private int agentUdpPort;
    private int agentTcpPort;
    private int managerUdpPort;
    private int managerTcpPort;
    private int remoteCommandPort;
    private int externalScanPort;
    
    // Session settings
    private int sessionRetrievingDelayMs;
    
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
    
    public int getRemoteCommandPort() {
        return remoteCommandPort;
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
    
    public void setRemoteCommandPort(int remoteCommandPort) {
        this.remoteCommandPort = remoteCommandPort;
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
    
    // ============================================================================
    //                           LEGACY CONSTANTS
    // ============================================================================
    // These are kept for backward compatibility but should use getters instead
    
    @Deprecated
    public String DATABASE_URL() {
        return databaseUrl;
    }
    
    @Deprecated  
    public String LANGUAGE() {
        return language;
    }
    
    @Deprecated
    public int AGENT_UDP_PORT() {
        return agentUdpPort;
    }
    
    @Deprecated
    public int AGENT_TCP_PORT() {
        return agentTcpPort;
    }
    
    @Deprecated
    public int MANAGER_UDP_PORT() {
        return managerUdpPort;
    }
    
    @Deprecated
    public int MANAGER_TCP_PORT() {
        return managerTcpPort;
    }
    
    @Deprecated
    public int REMOTE_COMMAND_PORT() {
        return remoteCommandPort;
    }
    
    @Deprecated
    public int EXTERNAL_SCAN_PORT() {
        return externalScanPort;
    }
    
    @Deprecated
    public int SESSION_RETRIEVING_DELAY_MS() {
        return sessionRetrievingDelayMs;
    }
}

