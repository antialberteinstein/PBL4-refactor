package service;

import model.Session;
import model.Process;
import util.ProtocolManager;

import java.util.ArrayList;
import java.util.List;
import util.Logger;

/**
 * Handles session information requests from Manager
 * Follows Single Responsibility Principle - only handles session protocol
 * Refactored to use SessionRetriever directly (no database)
 */
public class SessionSendingMonitor {

    private final ProtocolManager protocolManager;
    private final SessionRetriever sessionRetriever;

    public SessionSendingMonitor(ProtocolManager protocolManager, SessionRetriever sessionRetriever) {
        this.protocolManager = protocolManager;
        this.sessionRetriever = sessionRetriever;
    }

    public List<String> checkMessage(String message) {
        Logger.debug("SessionSendingMonitor", "Received message: " + message);
        String prefix = protocolManager.GET_SESSION_REQUEST;

        if (message.startsWith(prefix)) {
            // Collect session data ON-DEMAND (fresh data at request time)
            Session session = sessionRetriever.retrieveAndSaveSession();
            
            if (session == null) {
                return null; // Failed to collect session data
            }

            List<String> responses = new ArrayList<>();

            // Send session response
            String sessionResponse = parseSessionToProtocol(session);
            responses.add(sessionResponse);
            Logger.debug("SessionSendingMonitor", "Sending session response: " + sessionResponse);

            // Send all process responses
            List<Process> processes = session.getProcesses();
            Logger.debug("SessionSendingMonitor", "Number of processes to send: " + (processes != null ? processes.size() : 0));
            if (processes != null) {
                for (Process process : processes) {
                    String processResponse = parseProcessToProtocol(process);
                    responses.add(processResponse);
                    // Logger.debug("SessionSendingMonitor", "Sending process response: " + processResponse);
                }
            }

            return responses;
        }

        return null;
    }

    // Convert a Session object to protocol string for sending.
    private String parseSessionToProtocol(Session session) {
        String cpuUsagePrefix = "CPU_USAGE:";
        String totalRamPrefix = "TOTAL_RAM:";
        String ramUsagePrefix = "RAM_USAGE:";
        String macPrefix = "MAC:";
        String timestampPrefix = "TIMESTAMP:";

        StringBuilder sb = new StringBuilder();
        sb.append(protocolManager.GET_SESSION_RESPONSE).append(protocolManager.SEPARATOR);
        sb.append(macPrefix).append(session.getMacAddress()).append(protocolManager.SEPARATOR);
        sb.append(cpuUsagePrefix).append(session.getCpuUsage()).append(protocolManager.SEPARATOR);
        sb.append(totalRamPrefix).append(session.getTotalRam()).append(protocolManager.SEPARATOR);
        sb.append(ramUsagePrefix).append(session.getRamUsage()).append(protocolManager.SEPARATOR);
        sb.append(timestampPrefix).append(session.getTimestamp());
        return sb.toString();
    }

    // Convert a Process object to protocol string for sending.
    private String parseProcessToProtocol(Process process) {
        String macPrefix = "MAC:";
        String timestampPrefix = "TIMESTAMP:";
        String processPidPrefix = "PROCESS_PID:";
        String processNamePrefix = "PROCESS_NAME:";
        String processCpuUsagePrefix = "PROCESS_CPU_USAGE:";
        String processRamUsagePrefix = "PROCESS_RAM_USAGE:";

        StringBuilder sb = new StringBuilder();
        sb.append(protocolManager.PROCESS_RESPONSE).append(protocolManager.SEPARATOR);
        sb.append(macPrefix).append(process.getMacAddress()).append(protocolManager.SEPARATOR);
        sb.append(timestampPrefix).append(process.getTimestamp()).append(protocolManager.SEPARATOR);
        sb.append(processPidPrefix).append(process.getPid()).append(protocolManager.SEPARATOR);
        sb.append(processNamePrefix).append(process.getName()).append(protocolManager.SEPARATOR);
        sb.append(processCpuUsagePrefix).append(process.getCpuUsage()).append(protocolManager.SEPARATOR);
        sb.append(processRamUsagePrefix).append(process.getRamUsage());
        return sb.toString();
    }
}
