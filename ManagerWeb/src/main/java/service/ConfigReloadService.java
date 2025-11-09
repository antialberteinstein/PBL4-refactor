package service;

import config.AppConfig;
import config.ConfigManager;
import util.Messages;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ConfigReloadService - Automatically reload configuration when file changes
 * 
 * Monitors ~/PBL4DATA/config.json for changes and reloads when modified
 * This allows ManagerWeb to pick up changes made by Manager GUI without restart
 */
public class ConfigReloadService {
    
    private static final String COMPONENT = "ConfigReloadService";
    private static final long CHECK_INTERVAL_SECONDS = 5; // Check every 5 seconds
    
    private final ScheduledExecutorService scheduler;
    private long lastModified = 0;
    private AppConfig currentConfig;
    private ConfigChangeListener listener;
    
    /**
     * Interface for listening to config changes
     */
    public interface ConfigChangeListener {
        void onConfigChanged(AppConfig newConfig);
    }
    
    /**
     * Constructor
     * 
     * @param initialConfig Initial configuration
     */
    public ConfigReloadService(AppConfig initialConfig) {
        this.currentConfig = initialConfig;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ConfigReloadService");
            t.setDaemon(true); // Daemon thread won't prevent JVM shutdown
            return t;
        });
        
        // Get initial file modification time
        File configFile = new File(ConfigManager.getConfigFilePath());
        if (configFile.exists()) {
            this.lastModified = configFile.lastModified();
        }
    }
    
    /**
     * Start monitoring config file for changes
     */
    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkForChanges();
            } catch (Exception e) {
                System.err.println("[" + COMPONENT + "] Error checking config changes: " + e.getMessage());
            }
        }, CHECK_INTERVAL_SECONDS, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        
        System.out.println("[" + COMPONENT + "] Started monitoring config file (check interval: " + CHECK_INTERVAL_SECONDS + "s)");
    }
    
    /**
     * Stop monitoring
     */
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("[" + COMPONENT + "] Stopped monitoring config file");
        }
    }
    
    /**
     * Check if config file has been modified
     */
    private void checkForChanges() {
        File configFile = new File(ConfigManager.getConfigFilePath());
        
        if (!configFile.exists()) {
            return;
        }
        
        long currentModified = configFile.lastModified();
        
        // File has been modified since last check
        if (currentModified > lastModified) {
            System.out.println("[" + COMPONENT + "] Config file changed detected, reloading...");
            lastModified = currentModified;
            
            try {
                // Reload configuration
                AppConfig newConfig = ConfigManager.loadConfig();
                
                // Validate new config
                if (newConfig == null) {
                    System.err.println("[" + COMPONENT + "] Loaded config is null, keeping current config");
                    return;
                }
                
                // Check if language changed
                String oldLanguage = currentConfig.getLanguage();
                String newLanguage = newConfig.getLanguage();
                
                if (oldLanguage != null && newLanguage != null && !oldLanguage.equals(newLanguage)) {
                    System.out.println("[" + COMPONENT + "] Language changed: " + oldLanguage + " -> " + newLanguage);
                    Messages.setLanguage(newLanguage);
                }
                
                // Update current config
                this.currentConfig = newConfig;
                
                // Notify listener
                if (listener != null) {
                    listener.onConfigChanged(newConfig);
                }
                
                System.out.println("[" + COMPONENT + "] Configuration reloaded successfully");
                
            } catch (Exception e) {
                System.err.println("[" + COMPONENT + "] Error reloading config: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Set listener for config changes
     * 
     * @param listener Listener to notify when config changes
     */
    public void setConfigChangeListener(ConfigChangeListener listener) {
        this.listener = listener;
    }
    
    /**
     * Get current configuration
     * 
     * @return Current configuration
     */
    public AppConfig getCurrentConfig() {
        return currentConfig;
    }
    
    /**
     * Force reload configuration immediately
     */
    public void forceReload() {
        System.out.println("[" + COMPONENT + "] Force reload requested");
        checkForChanges();
    }
}
