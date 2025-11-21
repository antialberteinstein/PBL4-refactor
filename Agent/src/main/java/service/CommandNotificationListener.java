package service;

/**
 * CommandNotificationListener - Interface for receiving notifications about remote commands
 * Allows both GUI and CLI to display notifications
 */
public interface CommandNotificationListener {
    
    /**
     * Show notification about a remote command
     * 
     * @param title Notification title
     * @param message Notification message
     * @param type Notification type: "INFO", "WARNING", "ERROR", "SUCCESS"
     */
    void showNotification(String title, String message, String type);
    
    /**
     * Show kill process notification
     * 
     * @param pid Process ID
     * @param success Whether the kill was successful
     */
    void notifyKillProcess(int pid, boolean success);
    
    /**
     * Show shutdown notification
     * 
     * @param delay Delay in seconds
     */
    void notifyShutdown(int delay);
}
