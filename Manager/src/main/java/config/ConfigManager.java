package config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ConfigManager - Manages loading and saving configuration from JSON file
 * 
 * Configuration file location: ~/PBL4DATA/config.json
 * Database files location: ~/PBL4DATA/
 * - manager.db (main database)
 * - auth.db (authentication database)
 */
public class ConfigManager {
    
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/PBL4DATA";
    private static final String CONFIG_FILE = CONFIG_DIR + "/config.json";
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Load configuration from JSON file.
     * If file doesn't exist, create default configuration.
     * 
     * @return AppConfig instance with loaded settings
     */
    public static AppConfig loadConfig() {
        try {
            // Ensure config directory exists
            Path configDir = Paths.get(CONFIG_DIR);
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                System.out.println("Created configuration directory: " + CONFIG_DIR);
            }
            
            // Check if config file exists
            File configFile = new File(CONFIG_FILE);
            if (!configFile.exists()) {
                // Create default configuration
                System.out.println("Configuration file not found. Creating default configuration...");
                AppConfig defaultConfig = createDefaultConfig();
                saveConfig(defaultConfig);
                return defaultConfig;
            }
            
            // Load configuration from file
            try (FileReader reader = new FileReader(configFile)) {
                AppConfig config = gson.fromJson(reader, AppConfig.class);
                
                // Validate loaded config
                if (config == null) {
                    System.err.println("Configuration file is empty or invalid. Creating default...");
                    AppConfig defaultConfig = createDefaultConfig();
                    saveConfig(defaultConfig);
                    return defaultConfig;
                }
                
                System.out.println("Configuration loaded from: " + CONFIG_FILE);
                return config;
            }
            
        } catch (IOException e) {
            System.err.println("Error loading configuration: " + e.getMessage());
            System.out.println("Using default configuration...");
            return createDefaultConfig();
        } catch (com.google.gson.JsonSyntaxException e) {
            System.err.println("Invalid JSON syntax in config file: " + e.getMessage());
            System.out.println("Creating default configuration...");
            AppConfig defaultConfig = createDefaultConfig();
            saveConfig(defaultConfig);
            return defaultConfig;
        }
    }
    
    /**
     * Save configuration to JSON file
     * 
     * @param config Configuration to save
     * @return true if successful, false otherwise
     */
    public static boolean saveConfig(AppConfig config) {
        try {
            // Ensure config directory exists
            Path configDir = Paths.get(CONFIG_DIR);
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            
            // Write configuration to file
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                gson.toJson(config, writer);
                System.out.println("Configuration saved to: " + CONFIG_FILE);
                return true;
            }
            
        } catch (IOException e) {
            System.err.println("Error saving configuration: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Create default configuration with standard values
     */
    private static AppConfig createDefaultConfig() {
        String userHome = System.getProperty("user.home");
        String dataDir = userHome + "/PBL4DATA";
        
        AppConfig config = new AppConfig();
        config.setDatabaseUrl("jdbc:sqlite:" + dataDir + "/manager.db");
        config.setAuthDatabaseUrl("jdbc:sqlite:" + dataDir + "/auth.db");
        config.setEmailDatabaseUrl("jdbc:sqlite:" + dataDir + "/emails.db");
        
        // Agent ports (where Agent listens)
        config.setAgentScanComputerPort(5000);  // For HELLO and GET_COMPUTER_INFO
        config.setAgentSessionPort(5001);       // For GET_SESSION and processes
        config.setAgentUdpPort(5000);           // Legacy, maps to agentScanComputerPort
        config.setAgentTcpPort(4000);
        
        // Manager ports (where Manager listens)
        config.setManagerUdpPort(6000);         // Legacy, maps to managerScanPort
        config.setManagerScanPort(6000);        // For scan/HELLO responses
        config.setManagerSessionPort(6001);     // For session/computer/process data
        config.setManagerTcpPort(17000);
        config.setRemoteCommandPort(9999);
        config.setExternalScanPort(8888);
        
        config.setSessionRetrievingDelayMs(50);
        config.setCpuThresholdPercent(90.0);  // Default CPU threshold: 90%
        config.setRamThresholdPercent(90.0);  // Default RAM threshold: 90%
        config.setLanguage("en"); // Default to English
        
        return config;
    }    /**
     * Reload configuration from file and update the given config object
     * 
     * @param currentConfig Current configuration to update
     * @return Updated configuration
     */
    public static AppConfig reloadConfig(AppConfig currentConfig) {
        AppConfig newConfig = loadConfig();
        
        // Copy all values from new config to current config
        currentConfig.setDatabaseUrl(newConfig.getDatabaseUrl());
        currentConfig.setAuthDatabaseUrl(newConfig.getAuthDatabaseUrl());
        currentConfig.setEmailDatabaseUrl(newConfig.getEmailDatabaseUrl());
        currentConfig.setAgentScanComputerPort(newConfig.getAgentScanComputerPort());
        currentConfig.setAgentSessionPort(newConfig.getAgentSessionPort());
        currentConfig.setAgentTcpPort(newConfig.getAgentTcpPort());
        currentConfig.setManagerUdpPort(newConfig.getManagerUdpPort());
        currentConfig.setManagerScanPort(newConfig.getManagerScanPort());
        currentConfig.setManagerSessionPort(newConfig.getManagerSessionPort());
        currentConfig.setManagerTcpPort(newConfig.getManagerTcpPort());
        currentConfig.setRemoteCommandPort(newConfig.getRemoteCommandPort());
        currentConfig.setExternalScanPort(newConfig.getExternalScanPort());
        currentConfig.setSessionRetrievingDelayMs(newConfig.getSessionRetrievingDelayMs());
        currentConfig.setCpuThresholdPercent(newConfig.getCpuThresholdPercent());
        currentConfig.setRamThresholdPercent(newConfig.getRamThresholdPercent());
        currentConfig.setLanguage(newConfig.getLanguage());
        
        System.out.println("Configuration reloaded from file");
        return currentConfig;
    }
    
    /**
     * Get configuration directory path
     * 
     * @return Configuration directory path
     */
    public static String getConfigDirectory() {
        return CONFIG_DIR;
    }
    
    /**
     * Get configuration file path
     * 
     * @return Configuration file path
     */
    public static String getConfigFilePath() {
        return CONFIG_FILE;
    }
}
