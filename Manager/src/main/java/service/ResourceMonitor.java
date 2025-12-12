package service;

import config.AppConfig;
import database.ComputerManager;
import database.SessionManager;
import model.Computer;
import model.Session;
import util.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ResourceMonitor - Monitor CPU and RAM usage, send warnings when threshold exceeded
 * Runs as background thread checking all Agents periodically
 */
public class ResourceMonitor extends Thread {

    private static final String COMPONENT = "ResourceMonitor";
    private static final int CHECK_INTERVAL_MS = 5000; // Check every 5 seconds

    private final AppConfig appConfig;
    private final SessionManager sessionManager;
    private final ComputerManager computerManager;
    private final RemoteCommandClient remoteCommandClient;
    
    // Track which agents have been warned to avoid spamming
    private final Map<String, Boolean> cpuWarningsSent = new HashMap<>();
    private final Map<String, Boolean> ramWarningsSent = new HashMap<>();
    
    private boolean running = true;

    public ResourceMonitor(AppConfig appConfig, SessionManager sessionManager, 
                          ComputerManager computerManager, RemoteCommandClient remoteCommandClient) {
        this.appConfig = appConfig;
        this.sessionManager = sessionManager;
        this.computerManager = computerManager;
        this.remoteCommandClient = remoteCommandClient;
        setDaemon(true); // Run as daemon thread
    }

    /**
     * Stop monitoring
     */
    public void stopMonitoring() {
        running = false;
        interrupt();
    }

    @Override
    public void run() {
        Logger.info(COMPONENT, "Resource monitoring started (CPU: " + appConfig.getCpuThresholdPercent() + 
                    "%, RAM: " + appConfig.getRamThresholdPercent() + "%)");

        while (running) {
            try {
                checkAllAgents();
                Thread.sleep(CHECK_INTERVAL_MS);
            } catch (InterruptedException e) {
                if (running) {
                    Logger.debug(COMPONENT, "Monitoring interrupted");
                }
                break;
            } catch (Exception e) {
                Logger.error(COMPONENT, "Error in monitoring: " + e.getMessage());
            }
        }

        Logger.info(COMPONENT, "Resource monitoring stopped");
    }

    /**
     * Check all agents for threshold violations
     */
    private void checkAllAgents() {
        // Get all computers
        List<Computer> computers = computerManager.getAllComputers();
        
        for (Computer computer : computers) {
            String macAddress = computer.getMacAddress();
            String ip = computer.getIpAddress();
            
            // Get latest session for this computer
            Session latestSession = sessionManager.getLatestSessionByMac(macAddress);
            if (latestSession == null) {
                continue; // No session data yet
            }

            // Check CPU usage
            double cpuUsage = latestSession.getCpuUsage();
            if (cpuUsage > appConfig.getCpuThresholdPercent()) {
                handleCpuThresholdExceeded(computer, cpuUsage);
            } else {
                // Reset warning flag when usage drops below threshold
                cpuWarningsSent.put(macAddress, false);
            }

            // Check RAM usage
            double ramUsagePercent = calculateRamUsagePercent(latestSession);
            if (ramUsagePercent > appConfig.getRamThresholdPercent()) {
                handleRamThresholdExceeded(computer, ramUsagePercent);
            } else {
                // Reset warning flag when usage drops below threshold
                ramWarningsSent.put(macAddress, false);
            }
        }
    }

    /**
     * Calculate RAM usage percentage
     */
    private double calculateRamUsagePercent(Session session) {
        long totalRam = session.getTotalRam();
        long usedRam = session.getRamUsage();
        
        if (totalRam == 0) {
            return 0.0;
        }
        
        return (usedRam * 100.0) / totalRam;
    }

    /**
     * Handle CPU threshold exceeded
     */
    private void handleCpuThresholdExceeded(Computer computer, double cpuUsage) {
        String macAddress = computer.getMacAddress();
        String ip = computer.getIpAddress();
        String hostname = computer.getHostname();

        // Check if warning already sent for this agent
        if (cpuWarningsSent.getOrDefault(macAddress, false)) {
            return; // Already warned, don't spam
        }

        Logger.warn(COMPONENT, String.format("CPU threshold exceeded on %s (%s): %.2f%%", 
                    hostname, ip, cpuUsage));

        // Send warning to agent
        String message = String.format("WARNING: CPU usage is high: %.2f%% (Threshold: %.2f%%)", 
                                      cpuUsage, appConfig.getCpuThresholdPercent());
        
        try {
            remoteCommandClient.sendWarning(ip, message);
            cpuWarningsSent.put(macAddress, true);
            Logger.info(COMPONENT, "CPU warning sent to " + hostname);
        } catch (Exception e) {
            Logger.error(COMPONENT, "Failed to send CPU warning to " + hostname + ": " + e.getMessage());
        }
    }

    /**
     * Handle RAM threshold exceeded
     */
    private void handleRamThresholdExceeded(Computer computer, double ramUsagePercent) {
        String macAddress = computer.getMacAddress();
        String ip = computer.getIpAddress();
        String hostname = computer.getHostname();

        // Check if warning already sent for this agent
        if (ramWarningsSent.getOrDefault(macAddress, false)) {
            return; // Already warned, don't spam
        }

        Logger.warn(COMPONENT, String.format("RAM threshold exceeded on %s (%s): %.2f%%", 
                    hostname, ip, ramUsagePercent));

        // Send warning to agent
        String message = String.format("WARNING: RAM usage is high: %.2f%% (Threshold: %.2f%%)", 
                                      ramUsagePercent, appConfig.getRamThresholdPercent());
        
        try {
            remoteCommandClient.sendWarning(ip, message);
            ramWarningsSent.put(macAddress, true);
            Logger.info(COMPONENT, "RAM warning sent to " + hostname);
        } catch (Exception e) {
            Logger.error(COMPONENT, "Failed to send RAM warning to " + hostname + ": " + e.getMessage());
        }
    }
}
