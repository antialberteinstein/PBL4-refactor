package service;

import util.ProtocolManager;
import model.Computer;

/**
 * Handles computer information requests from Manager
 * Follows Single Responsibility Principle - only handles computer info protocol
 * Refactored to use ComputerRetriever directly
 */
public class ComputerSendingMonitor {
    private final ProtocolManager protocolManager;
    private final ComputerRetriever computerRetriever;

    public ComputerSendingMonitor(ProtocolManager protocolManager, ComputerRetriever computerRetriever) {
        this.protocolManager = protocolManager;
        this.computerRetriever = computerRetriever;
    }

    public String checkMessage(String message) {
        String prefix = protocolManager.GET_COMPUTER_INFO_REQUEST;

        if (message.startsWith(prefix)) {
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
