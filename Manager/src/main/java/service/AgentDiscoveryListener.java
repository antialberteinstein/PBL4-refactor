package service;

/**
 * Callback interface for notifying when a new Agent is discovered
 * This allows SessionRetriever to notify without knowing about ManagerMain
 * Follows Dependency Inversion Principle - service layer depends on abstraction, not concrete class
 */
public interface AgentDiscoveryListener {
    
    /**
     * Called when a new Agent's computer info is successfully saved to database
     * 
     * @param macAddress The MAC address of the newly discovered Agent
     * @param ipAddress The IP address of the newly discovered Agent
     */
    void onAgentDiscovered(String macAddress, String ipAddress);
}
