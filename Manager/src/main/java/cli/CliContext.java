package cli;

/**
 * CLI Context - Maintains the current state for "use" commands
 * Similar to MySQL's USE database and USE table context
 */
public class CliContext {
    
    private String currentAgent; // Currently selected Agent MAC address
    private Integer currentSession; // Currently selected Session ID
    
    public CliContext() {
        this.currentAgent = null;
        this.currentSession = null;
    }
    
    /**
     * Set the current Agent context (via "use agent <MAC>")
     */
    public void useAgent(String macAddress) {
        this.currentAgent = macAddress;
        this.currentSession = null; // Reset session when changing agent
    }
    
    /**
     * Set the current Session context (via "use session <ID>")
     */
    public void useSession(Integer sessionId) {
        this.currentSession = sessionId;
    }
    
    /**
     * Clear all context (back to root)
     */
    public void clear() {
        this.currentAgent = null;
        this.currentSession = null;
    }
    
    /**
     * Get current Agent MAC address
     */
    public String getCurrentAgent() {
        return currentAgent;
    }
    
    /**
     * Get current Session ID
     */
    public Integer getCurrentSession() {
        return currentSession;
    }
    
    /**
     * Check if an Agent is currently selected
     */
    public boolean hasAgent() {
        return currentAgent != null;
    }
    
    /**
     * Check if a Session is currently selected
     */
    public boolean hasSession() {
        return currentSession != null;
    }
    
    /**
     * Get context prompt string (like MySQL shows current database)
     */
    public String getPrompt() {
        if (currentSession != null) {
            return "Manager [session:" + currentSession + "]> ";
        } else if (currentAgent != null) {
            return "Manager [" + currentAgent + "]> ";
        } else {
            return "Manager> ";
        }
    }
}
