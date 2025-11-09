package service;

import model.Computer;
import util.Logger;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Service for retrieving computer information using OSHI library
 * Data is stored in-memory, not in database
 * Refactored to remove database dependency
 */
public class ComputerRetriever implements Runnable {
    
    private static final String COMPONENT = "ComputerRetriever";
    private static final SystemInfo systemInfo = new SystemInfo();
    private Computer currentComputer = null;

    /**
     * Constructor
     */
    public ComputerRetriever() {
    }

    public Computer retrieveAndSaveComputer() {
        try {
            Computer com = new Computer();

            // Get hostname & ip.
            InetAddress addr = InetAddress.getLocalHost();
            com.setHostname(addr.getHostName());
            String ipAddress = addr.getHostAddress();

            // Neu ipAddress la loopback.
            if ("127.0.0.1".equals(ipAddress) || "::1".equals(ipAddress)) {
                ipAddress = getNetworkIpAddress();
            }
            com.setIpAddress(ipAddress);

            // Get MAC Address.
            com.setMacAddress(getMacAddress());

            // Get operation system name and architecture.
            String osName = System.getProperty("os.name");
            String osVersion = System.getProperty("os.version");
            String archi = System.getProperty("os.arch");
            com.setOs(osName + " " + osVersion);
            com.setArchitecture(archi);

            // Setup hardware information.
            HardwareAbstractionLayer hal = systemInfo.getHardware();
            ComputerSystem computerSystem = hal.getComputerSystem();
            com.setManufacturer(computerSystem.getManufacturer());  // Hang san xuat.
            com.setModel(computerSystem.getModel());  // Dong may.
            com.setSerialNumber(computerSystem.getSerialNumber());  // So serial.

            // CPU information.
            CentralProcessor processor = hal.getProcessor();
            com.setCpuName(processor.getProcessorIdentifier().getName()); // Ten CPU, vd M2.
            com.setCpuVendor(processor.getProcessorIdentifier().getVendor());  // Ten hang san xuat, vd Apple.
            com.setPhysicalCores(processor.getPhysicalProcessorCount());  // So nhan vat ly, vd 8.
            com.setLogicalCores(processor.getLogicalProcessorCount());  // So luong xu ly moi nhan.
            com.setCpuMaxFreq(processor.getMaxFreq());  // Tan so toi da cua CPU.

            // Store in memory
            this.currentComputer = com;

            return com;
        } catch (Exception ex) {
            Logger.error(COMPONENT, "Error retrieving computer information", ex);
        }

        return null;
    }

    public void run() {
        retrieveAndSaveComputer();
    }

    public Computer getCurrentComputer() {
        if (currentComputer == null) {
            retrieveAndSaveComputer();
        }
        return currentComputer;
    }

    private static String getMacAddress() {
        try {
            // Try to get MAC address from first non-loopback network interface
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                
                // Skip loopback and inactive interfaces
                if (network.isLoopback() || !network.isUp()) {
                    continue;
                }
                
                byte[] mac = network.getHardwareAddress();
                if (mac != null && mac.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                    }
                    String macAddress = sb.toString();
                    Logger.debug(COMPONENT, "Found MAC address: " + macAddress + " for interface: " + network.getName());
                    return macAddress;
                }
            }
            
            Logger.warn(COMPONENT, "No MAC address found for any network interface");
        } catch (Exception e) {
            Logger.error(COMPONENT, "Error getting MAC address", e);
        }
        return "Unknown";
    }

    private static String getNetworkIpAddress() {
        try {
            Enumeration<java.net.NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                // Skip loopback and inactive interfaces
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<java.net.InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress address = addresses.nextElement();
                    
                    // Skip loopback, link-local, and IPv6 addresses
                    if (address.isLoopbackAddress() || address.isLinkLocalAddress() || 
                        address instanceof java.net.Inet6Address) {
                        continue;
                    }
                    
                    // Return the first valid IPv4 address
                    String ip = address.getHostAddress();
                    if (ip != null && !ip.startsWith("127.") && !ip.startsWith("169.254.")) {
                        Logger.debug(COMPONENT, "Found network IP: " + ip + " on interface: " + networkInterface.getName());
                        return ip;
                    }
                }
            }
        } catch (Exception e) {
            Logger.warn(COMPONENT, "Error getting network IP address: " + e.getMessage());
        }
        return "Unknown";
    }
}
