package config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ConfigManager - Manage application configuration
 * 
 * Shared configuration between Manager GUI and ManagerWeb
 * Configuration file: ~/PBL4DATA/config.json
 */
public class ConfigManager {
    
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/PBL4DATA";
    private static final String CONFIG_FILE = CONFIG_DIR + "/config.json";
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Load configuration from file or create default
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
     * Save configuration to file
     * 
     * @param config Configuration to save
     */
    public static void saveConfig(AppConfig config) {
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
            }
            
        } catch (IOException e) {
            System.err.println("Error saving configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Reload configuration from file
     * 
     * @return Reloaded configuration
     */
    public static AppConfig reloadConfig(AppConfig currentConfig) {
        AppConfig newConfig = loadConfig();
        
        // Copy all values from new config to current config
        currentConfig.setDatabaseUrl(newConfig.getDatabaseUrl());
        currentConfig.setAuthDatabaseUrl(newConfig.getAuthDatabaseUrl());
        currentConfig.setEmailDatabaseUrl(newConfig.getEmailDatabaseUrl());
        currentConfig.setAgentUdpPort(newConfig.getAgentUdpPort());
        currentConfig.setAgentTcpPort(newConfig.getAgentTcpPort());
        currentConfig.setManagerUdpPort(newConfig.getManagerUdpPort());
        currentConfig.setManagerTcpPort(newConfig.getManagerTcpPort());
        currentConfig.setExternalScanPort(newConfig.getExternalScanPort());
        currentConfig.setSessionRetrievingDelayMs(newConfig.getSessionRetrievingDelayMs());
        currentConfig.setLanguage(newConfig.getLanguage());
        
        System.out.println("Configuration reloaded from file");
        return currentConfig;
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
        
        // Agent ports
        config.setAgentUdpPort(5000);
        config.setAgentTcpPort(4000);
        config.setManagerUdpPort(6000);
        config.setManagerTcpPort(7000);
        config.setExternalScanPort(8888);
        config.setSessionRetrievingDelayMs(50);
        config.setLanguage("en"); // Default to English
        
        return config;
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
