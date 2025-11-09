package service;


import model.Computer;
import model.Process;
import model.Session;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import util.Logger;

import java.util.List;
import java.util.ArrayList;

/**
 * Service for retrieving session information
 * Data is stored in-memory, not in database
 * Refactored to remove database dependency
 */
public class SessionRetriever implements Runnable {

    private static final String COMPONENT = "SessionRetriever";
    private static final SystemInfo systemInfo = new SystemInfo();
    private final ComputerRetriever computerRetriever;
    private Session currentSession;

    /**
     * Constructor with dependency injection
     * @param computerRetriever The computer retriever instance
     */
    public SessionRetriever(ComputerRetriever computerRetriever) {
        this.computerRetriever = computerRetriever;
    }

    public Session retrieveAndSaveSession() {
        try {
            Session session = new Session();

            // Set MAC address.
            Computer com = computerRetriever.getCurrentComputer();
            if (com == null) {
                com = computerRetriever.retrieveAndSaveComputer();
            }
            session.setMacAddress(com.getMacAddress());

            // Get CPU information.
            HardwareAbstractionLayer hal = systemInfo.getHardware();
            CentralProcessor processor = hal.getProcessor();
            double cpuLoad = processor.getSystemCpuLoad(1000);  // Lấy trung bình trong 1 giây.

            // Handle case where CPU load cannot be determined immediately
            if (cpuLoad < 0) {
                // Fallback: try to get current CPU load without waiting
                cpuLoad = processor.getSystemCpuLoad(0);
                if (cpuLoad < 0) {
                    // Final fallback: estimate from processor ticks
                    long[] ticks = processor.getSystemCpuLoadTicks();
                    cpuLoad = 1.0 - ((double) ticks[oshi.hardware.CentralProcessor.TickType.IDLE.getIndex()] / 
                                    java.util.Arrays.stream(ticks).sum());
                }
            }
            
            // Ensure CPU load is within valid range
            if (cpuLoad < 0) {
                cpuLoad = 0.0; // Default to 0 if still can't get CPU load
            }
            
            session.setCpuUsage(cpuLoad * 100); // Convert to percentage

            // Get memory information
            GlobalMemory memory = hal.getMemory();
            long totalRam = memory.getTotal();
            long availableRam = memory.getAvailable();
            session.setTotalRam(totalRam);
            session.setRamUsage(totalRam - availableRam);

            // Get ALL process information
            List<Process> processes = new ArrayList<>();
            OperatingSystem os = systemInfo.getOperatingSystem();
            List<OSProcess> osProcesses = os.getProcesses();
            
            for (OSProcess osProc : osProcesses) {
                Process process = new Process(
                    osProc.getProcessID(),
                    session.getMacAddress(),
                    session.getTimestamp(),
                    osProc.getName(),
                    osProc.getProcessCpuLoadCumulative() * 100,
                    osProc.getResidentSetSize()
                );
                processes.add(process);
            }
            session.setProcesses(processes);

            // Store in memory
            this.currentSession = session;

            return session;
        } catch (Exception ex) {
            Logger.error(COMPONENT, "Exception while retrieving session data: " + ex.getMessage(), ex);
        }

        return null;
    }

    public void run() {
        Session session = retrieveAndSaveSession();

        if (session != null) {
            Logger.debug(COMPONENT, "Session recorded with CPU usage: " + session.getCpuUsage() + "%");
            Logger.debug(COMPONENT, "RAM usage: " + session.getRamUsage() + "/" + session.getTotalRam() + "(" + ((double)session.getRamUsage()/session.getTotalRam()*100) + "%)");
            Logger.debug(COMPONENT, "Number of processes: " + session.getProcesses().size());
        } else {
            Logger.warn(COMPONENT, "Session data collection failed");
        }
    }

    public Session getCurrentSession() {
        if (currentSession == null) {
            retrieveAndSaveSession();
        }
        return currentSession;
    }

}
