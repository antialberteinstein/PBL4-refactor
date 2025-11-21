package service;

import model.Session;
import model.Process;
import util.ProtocolManager;
import util.Logger;
import config.AppConfig;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.util.List;

import java.net.InetAddress;

/**
 * Handles session and process information requests from Manager
 * Independent thread with its own UDP mailbox
 * Uses dedicated port AGENT_SESSION_PORT for both receiving and sending
 */
public class SessionSendingMonitor extends Thread {

    private static final String COMPONENT = "SessionSendingMonitor";

    private final ProtocolManager protocolManager;
    private final SessionRetriever sessionRetriever;
    private final AppConfig appConfig;
    private DatagramSocket mailbox;  // Single mailbox for all session/process communication
    private boolean running = true;

    public SessionSendingMonitor(ProtocolManager protocolManager, SessionRetriever sessionRetriever, 
                                 AppConfig appConfig) {
        this.protocolManager = protocolManager;
        this.sessionRetriever = sessionRetriever;
        this.appConfig = appConfig;
    }

    /**
     * Open UDP socket for session/process communication
     */
    public void open() throws Exception {
        if (mailbox == null || mailbox.isClosed()) {
            mailbox = new DatagramSocket(appConfig.AGENT_SESSION_PORT);
            Logger.info(COMPONENT, "Opened UDP socket on port " + appConfig.AGENT_SESSION_PORT);
        }
    }

    /**
     * Close UDP socket
     */
    public void close() {
        running = false;
        if (mailbox != null && !mailbox.isClosed()) {
            mailbox.close();
            Logger.info(COMPONENT, "Closed UDP socket");
        }
    }

    /**
     * Main message processing loop
     * Listens for GET_SESSION_REQUEST and responds with session data
     * Delegates process sending to ProcessSendingMonitor
     */
    @Override
    public void run() {
        Logger.info(COMPONENT, "Started listening for session requests");
        
        while (running) {
            try {
                // Receive UDP packet
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                mailbox.receive(packet);
                
                String message = new String(packet.getData(), 0, packet.getLength());
                Logger.debug(COMPONENT, "Received message: " + message);
                
                // Check if it's a GET_SESSION_REQUEST
                String response = checkMessage(message, packet);
                if (response != null) {
                    // Send session response
                    byte[] responseBytes = response.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(
                        responseBytes, 
                        responseBytes.length,
                        packet.getAddress(),
                        packet.getPort()
                    );
                    mailbox.send(responsePacket);
                    Logger.debug(COMPONENT, "Sent session response");
                    
                    // Note: Processes are sent by separate thread with delay (see checkMessage)
                }
                
            } catch (Exception e) {
                if (running) {
                    Logger.error(COMPONENT, "Error processing message: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        close();
        Logger.info(COMPONENT, "Stopped");
    }

    /**
     * Check if message is a session request and generate response
     * Also sends processes after session with delay
     */
    private String checkMessage(String message, DatagramPacket packet) {
        Logger.debug(COMPONENT, "Checking message: " + message);
        String prefix = protocolManager.GET_SESSION_REQUEST;

        if (message.startsWith(prefix)) {
            // Collect session data ON-DEMAND (fresh data at request time)
            Session session = sessionRetriever.retrieveAndSaveSession();
            
            if (session == null) {
                return null; // Failed to collect session data
            }

            // Prepare session response
            String sessionResponse = parseSessionToProtocol(session);
            Logger.debug(COMPONENT, "Prepared session response: " + sessionResponse);

            // Schedule process sending in separate thread after session is sent
            List<Process> processes = session.getProcesses();
            if (processes != null && !processes.isEmpty()) {
                final List<Process> processesToSend = processes;
                final InetAddress addr = packet.getAddress();
                final int port = packet.getPort();
                
                new Thread(() -> {
                    try {
                        // Wait for session to be received and saved by Manager
                        Thread.sleep(100); // 100ms delay
                        sendProcesses(processesToSend, addr, port);
                    } catch (InterruptedException e) {
                        Logger.debug(COMPONENT, "Process sending interrupted");
                    }
                }).start();
            }

            return sessionResponse;
        }

        return null;
    }

    /**
     * Send process list to Manager via same mailbox
     */
    private void sendProcesses(List<Process> processes, InetAddress managerAddress, int managerPort) {
        if (processes == null || processes.isEmpty()) {
            Logger.debug(COMPONENT, "No processes to send");
            return;
        }

        try {
            Logger.debug(COMPONENT, "Sending " + processes.size() + " processes to Manager");
            
            // Parse and send each process
            for (Process process : processes) {
                String processResponse = parseProcessToProtocol(process);
                sendProcessToManager(processResponse, managerAddress, managerPort);
            }
            
            Logger.debug(COMPONENT, "Successfully sent " + processes.size() + " process responses");
            
        } catch (Exception e) {
            Logger.error(COMPONENT, "Error sending processes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send a process message to Manager via same mailbox
     */
    private void sendProcessToManager(String message, InetAddress address, int port) throws Exception {
        byte[] messageBytes = message.getBytes();
        DatagramPacket packet = new DatagramPacket(
            messageBytes, 
            messageBytes.length,
            address,
            port
        );
        mailbox.send(packet);
    }

    /**
     * Convert a Process object to protocol string for sending
     */
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
}
