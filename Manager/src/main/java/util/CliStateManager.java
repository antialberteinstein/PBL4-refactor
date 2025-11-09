package util;

import model.Computer;

import java.util.*;

/**
 * CliStateManager - Manages agent IDs and mappings for CLI
 * 
 * Provides sequential IDs (1, 2, 3, ...) for agents to make CLI usage easier.
 * Users can reference agents by either ID or MAC address.
 */
public class CliStateManager {
    
    private static CliStateManager instance;
    
    // Maps: ID -> MAC address
    private final Map<Integer, String> idToMacMap;
    
    // Maps: MAC address -> ID
    private final Map<String, Integer> macToIdMap;
    
    // Counter for generating sequential IDs
    private int nextId;
    
    private CliStateManager() {
        this.idToMacMap = new LinkedHashMap<>();
        this.macToIdMap = new HashMap<>();
        this.nextId = 1;
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized CliStateManager getInstance() {
        if (instance == null) {
            instance = new CliStateManager();
        }
        return instance;
    }
    
    /**
     * Register or update agents from the list
     * Assigns IDs to new agents, maintains existing IDs
     * 
     * @param agents List of agents to register
     */
    public void updateAgents(List<Computer> agents) {
        // Remove agents that are no longer in the list
        Set<String> currentMacs = new HashSet<>();
        for (Computer agent : agents) {
            currentMacs.add(agent.getMacAddress());
        }
        
        // Remove old agents
        Iterator<Map.Entry<String, Integer>> iterator = macToIdMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            if (!currentMacs.contains(entry.getKey())) {
                int id = entry.getValue();
                idToMacMap.remove(id);
                iterator.remove();
            }
        }
        
        // Add new agents
        for (Computer agent : agents) {
            String mac = agent.getMacAddress();
            if (!macToIdMap.containsKey(mac)) {
                int id = nextId++;
                macToIdMap.put(mac, id);
                idToMacMap.put(id, mac);
            }
        }
    }
    
    /**
     * Get MAC address by ID
     * 
     * @param id Agent ID
     * @return MAC address or null if not found
     */
    public String getMacById(int id) {
        return idToMacMap.get(id);
    }
    
    /**
     * Get ID by MAC address
     * 
     * @param mac MAC address
     * @return ID or null if not found
     */
    public Integer getIdByMac(String mac) {
        return macToIdMap.get(mac);
    }
    
    /**
     * Check if an ID exists
     */
    public boolean hasId(int id) {
        return idToMacMap.containsKey(id);
    }
    
    /**
     * Check if a MAC address is registered
     */
    public boolean hasMac(String mac) {
        return macToIdMap.containsKey(mac);
    }
    
    /**
     * Get all registered IDs in order
     */
    public List<Integer> getAllIds() {
        return new ArrayList<>(idToMacMap.keySet());
    }
    
    /**
     * Get the total number of registered agents
     */
    public int getAgentCount() {
        return macToIdMap.size();
    }
    
    /**
     * Resolve agent identifier (can be ID or MAC address)
     * Returns MAC address
     * 
     * @param identifier Either an ID (e.g., "1", "2") or MAC address
     * @return MAC address or null if not found
     */
    public String resolveAgent(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return null;
        }
        
        // Try to parse as ID first
        try {
            int id = Integer.parseInt(identifier);
            String mac = getMacById(id);
            if (mac != null) {
                return mac;
            }
        } catch (NumberFormatException e) {
            // Not a number, treat as MAC address
        }
        
        // Treat as MAC address
        if (hasMac(identifier)) {
            return identifier;
        }
        
        return null;
    }
    
    /**
     * Clear all mappings (useful for testing or reset)
     */
    public void clear() {
        idToMacMap.clear();
        macToIdMap.clear();
        nextId = 1;
    }
    
    /**
     * Get a formatted string showing the ID-MAC mapping
     */
    public String getMapping() {
        if (macToIdMap.isEmpty()) {
            return "No agents registered.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Agent ID Mappings:\n");
        
        List<Integer> sortedIds = new ArrayList<>(idToMacMap.keySet());
        Collections.sort(sortedIds);
        
        for (int id : sortedIds) {
            String mac = idToMacMap.get(id);
            sb.append(String.format("  ID %d -> %s\n", id, mac));
        }
        
        return sb.toString();
    }
}
