package service;

import util.ProtocolManager;
import util.Logger;
import config.AppConfig;
import model.Computer;

import java.net.DatagramSocket;
import java.net.DatagramPacket;

/**
 * Handles computer-related requests from Manager
 * Now runs as independent thread with its own UDP mailbox
 * Handles both HELLO_REQUEST (scanning) and GET_COMPUTER_INFO_REQUEST
 * Uses port AGENT_SCAN_COMPUTER_PORT
 */
public class ComputerSendingMonitor extends Thread {
    
    private static final String COMPONENT = "ComputerSendingMonitor";
    
    private final ProtocolManager protocolManager;
    private final ComputerRetriever computerRetriever;
    private final AppConfig appConfig;
    private DatagramSocket mailbox;
    private boolean running = true;

    public ComputerSendingMonitor(ProtocolManager protocolManager, ComputerRetriever computerRetriever, AppConfig appConfig) {
        this.protocolManager = protocolManager;
        this.computerRetriever = computerRetriever;
        this.appConfig = appConfig;
    }

    /**
     * Open UDP socket on AGENT_SCAN_COMPUTER_PORT
     */
    public void open() throws Exception {
        if (mailbox == null || mailbox.isClosed()) {
            mailbox = new DatagramSocket(appConfig.AGENT_SCAN_COMPUTER_PORT);
            Logger.info(COMPONENT, "Opened UDP socket on port " + appConfig.AGENT_SCAN_COMPUTER_PORT);
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
     * Listens for both HELLO_REQUEST and GET_COMPUTER_INFO_REQUEST
     */
    @Override
    public void run() {
        Logger.info(COMPONENT, "Started listening for computer-related requests");
        
        while (running) {
            try {
                // Receive UDP packet
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                mailbox.receive(packet);
                
                String message = new String(packet.getData(), 0, packet.getLength());
                Logger.debug(COMPONENT, "Received message: " + message);
                
                // Check message type and generate response
                String response = checkMessage(message);
                if (response != null) {
                    // Send response back
                    byte[] responseBytes = response.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(
                        responseBytes, 
                        responseBytes.length,
                        packet.getAddress(),
                        packet.getPort()
                    );
                    mailbox.send(responsePacket);
                    Logger.debug(COMPONENT, "Sent response");
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
     * Check if message is a computer-related request and generate response
     * Handles both HELLO_REQUEST (scanning) and GET_COMPUTER_INFO_REQUEST
     */
    private String checkMessage(String message) {
        // Check for HELLO_REQUEST (scanning)
        if (message.startsWith(protocolManager.HELLO_REQUEST)) {
            Computer computer = computerRetriever.getCurrentComputer();
            return protocolManager.HELLO_RESPONSE + protocolManager.SEPARATOR + computer.getIpAddress();
        }
        
        // Check for GET_COMPUTER_INFO_REQUEST
        if (message.startsWith(protocolManager.GET_COMPUTER_INFO_REQUEST)) {
            Computer com = computerRetriever.retrieveAndSaveComputer();
            if (com == null) {
                return null; // No computer info available yet
            }
            return parseComputerToProtocol(com);
        }

        return null;
    }

    private String parseComputerToProtocol(Computer com) {
        // Thông tin cơ bản
        String hostNamePrefix = "HOSTNAME:";
        String ipAddressPrefix = "IP:";
        String macAddressPrefix = "MAC:";
        String osPrefix = "OS:";
        String architecturePrefix = "ARCH:";

        // Hardware info
        String manufacturerPrefix = "MANUFACTURER:";
        String modelPrefix = "MODEL:";
        String serialNumberPrefix = "SERIAL:";

        // CPU
        String cpuNamePrefix = "CPU_NAME:";
        String cpuVendorPrefix = "CPU_VENDOR:";
        String physicalCoresPrefix = "PHYSICAL_CORES:";
        String logicalCoresPrefix = "LOGICAL_CORES:";
        String cpuMaxFreqPrefix = "CPU_MAX_FREQ:";

        StringBuilder sb = new StringBuilder();
        sb.append(protocolManager.GET_COMPUTER_INFO_RESPONSE).append(protocolManager.SEPARATOR);
        sb.append(hostNamePrefix).append(com.getHostname()).append(protocolManager.SEPARATOR);
        sb.append(ipAddressPrefix).append(com.getIpAddress()).append(protocolManager.SEPARATOR);
        sb.append(macAddressPrefix).append(com.getMacAddress()).append(protocolManager.SEPARATOR);
        sb.append(osPrefix).append(com.getOs()).append(protocolManager.SEPARATOR);
        sb.append(architecturePrefix).append(com.getArchitecture()).append(protocolManager.SEPARATOR);
        sb.append(manufacturerPrefix).append(com.getManufacturer()).append(protocolManager.SEPARATOR);
        sb.append(modelPrefix).append(com.getModel()).append(protocolManager.SEPARATOR);
        sb.append(serialNumberPrefix).append(com.getSerialNumber()).append(protocolManager.SEPARATOR);
        sb.append(cpuNamePrefix).append(com.getCpuName()).append(protocolManager.SEPARATOR);
        sb.append(cpuVendorPrefix).append(com.getCpuVendor()).append(protocolManager.SEPARATOR);
        sb.append(physicalCoresPrefix).append(com.getPhysicalCores()).append(protocolManager.SEPARATOR);
        sb.append(logicalCoresPrefix).append(com.getLogicalCores()).append(protocolManager.SEPARATOR);
        sb.append(cpuMaxFreqPrefix).append(com.getCpuMaxFreq()).append(protocolManager.SEPARATOR);

        return sb.toString();
    }
}
