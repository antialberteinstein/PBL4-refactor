package service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import util.Logger;

import config.AppConfig;
import util.ProtocolManager;

/**
 * Service for scanning and discovering hosts on the network
 * Independent UDP mailbox for receiving HELLO responses
 */
public class HostScanner extends Thread {

    private static final String COMPONENT = "HostScanner";

    private AppConfig appConfig;
    private ProtocolManager protocolManager;
    private DatagramSocket mailbox;
    private List<String> hosts;
    private SessionRetriever sessionRetriever; // For requesting computer info after discovery

    public HostScanner(AppConfig appConfig, ProtocolManager protocolManager) {
        this.appConfig = appConfig;
        this.protocolManager = protocolManager;
        this.hosts = new CopyOnWriteArrayList<>();
    }
    
    public void setSessionRetriever(SessionRetriever sessionRetriever) {
        this.sessionRetriever = sessionRetriever;
    }

    public void open() throws Exception {
        if (mailbox == null || mailbox.isClosed()) {
            mailbox = new DatagramSocket(appConfig.getManagerScanPort());
            Logger.debug(COMPONENT, "HostScanner mailbox opened on port " + appConfig.getManagerScanPort());
        }
    }

    public void close() {
        if (mailbox != null && !mailbox.isClosed()) {
            mailbox.close();
            Logger.debug(COMPONENT, "HostScanner mailbox closed");
        }
    }

    // Thread run method to continuously receive HELLO responses
    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                mailbox.receive(packet);
                
                String message = new String(packet.getData(), 0, packet.getLength());
                checkMessage(message);
                
            } catch (java.net.SocketException e) {
                if (isInterrupted() || mailbox.isClosed()) {
                    break;
                }
            } catch (Exception e) {
                if (isInterrupted()) {
                    break;
                }
            }
        }
    }

    public void scan() {
        try {
            
            // Lấy địa chỉ IP cục bộ và subnet mask
            String localIp = getIpAddress();
            String subnetMask = getSubnetMask();

            Logger.debug(COMPONENT, "Local IP: " + localIp + ", Subnet Mask: " + subnetMask);

            // Chuyển địa chỉ IP và subnet mask sang dạng số nguyên
            int ipInt = ipToInt(localIp);
            int maskInt = ipToInt(subnetMask);

            // Tính địa chỉ mạng và broadcast
            int networkInt = ipInt & maskInt;
            int broadcastInt = networkInt | ~maskInt;

            String broadcastIp = intToIp(broadcastInt);
            Logger.debug(COMPONENT, "Sending HELLO to broadcast address: " + broadcastIp);
            
            // Send HELLO request directly using own mailbox
            String helloMessage = protocolManager.HELLO_REQUEST;
            InetAddress receiverAddress = InetAddress.getByName(broadcastIp);
            byte[] messageBytes = helloMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, 
                                                       receiverAddress, appConfig.getAgentScanComputerPort());
            mailbox.send(packet);
            
        } catch (Exception e) {
            Logger.error(COMPONENT, "Error scanning hosts: " + e.getMessage());
        }
    }

    public String checkMessage(String message) {
        String prefix = protocolManager.HELLO_RESPONSE + protocolManager.SEPARATOR;
        if (message.startsWith(prefix)) {
            String ip = message.substring(prefix.length());
            if (!hosts.contains(ip)) {
                hosts.add(ip);
                Logger.info(COMPONENT, "Discovered host: " + ip);
                
                // Request computer info from newly discovered Agent
                if (sessionRetriever != null) {
                    try {
                        sessionRetriever.sendGetComputerInfoRequest(ip);
                    } catch (Exception e) {
                        Logger.error(COMPONENT, "Failed to request computer info from " + ip + ": " + e.getMessage());
                    }
                }
            }
            return ip;
        }
        return null;
    }

    public List<String> getHosts() {
        return new ArrayList<>(hosts);
    }

    public DatagramSocket getMailbox() {
        return mailbox;
    }
    
    /**
     * Remove a host from the discovered list.
     * This allows the host to be re-discovered in subsequent scans.
     * 
     * @param ipAddress IP address to remove
     */
    public void removeHost(String ipAddress) {
        if (hosts.remove(ipAddress)) {
            Logger.info(COMPONENT, "Removed host from discovery cache: " + ipAddress);
        }
    }
    
    /**
     * Clear the entire discovery cache.
     * Forces re-discovery of all hosts on next scan.
     */
    public void clearCache() {
        hosts.clear();
        Logger.info(COMPONENT, "Cleared discovery cache");
    }

    // Hàm chuyển IP string -> int
    private static int ipToInt(String ip) throws Exception {
        byte[] bytes = InetAddress.getByName(ip).getAddress();
        int result = 0;
        for (byte b : bytes) {
            result = (result << 8) | (b & 0xFF);
        }
        return result;
    }

    // Hàm chuyển int -> IP string
    private static String intToIp(int value) throws Exception {
        return String.format("%d.%d.%d.%d",
                (value >>> 24) & 0xFF,
                (value >>> 16) & 0xFF,
                (value >>> 8) & 0xFF,
                value & 0xFF);
    }

    private static String getIpAddress() throws Exception {
        InetAddress addr = InetAddress.getLocalHost();
        String ip = addr.getHostAddress();

        if ("127.0.0.1".equals(ip) || "::1".equals(ip)) {
            ip = getNetworkIpAddress();
        }
        return ip;
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

    private static String getSubnetMask() {
        try {
            // Lấy IP hiện tại
            String localIp = getIpAddress();
            InetAddress currentInet = InetAddress.getByName(localIp);

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                // Bỏ qua loopback hoặc interface không hoạt động
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                    InetAddress inetAddress = address.getAddress();

                    // Chỉ quan tâm IPv4
                    if (inetAddress instanceof Inet4Address) {
                        // Nếu IP này trùng với IP cục bộ
                        if (inetAddress.equals(currentInet)) {
                            short prefixLength = address.getNetworkPrefixLength();
                            String subnetMask = prefixLengthToSubnetMask(prefixLength);
                            Logger.debug(COMPONENT, "Subnet mask for " + localIp + " is " + subnetMask);
                            return subnetMask;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.warn(COMPONENT, "Error getting subnet mask: " + e.getMessage());
        }
        return "Unknown";
    }

    // Hàm chuyển prefix length (vd: 24) thành subnet mask (vd: 255.255.255.0)
    private static String prefixLengthToSubnetMask(short prefixLength) {
        int mask = 0xffffffff << (32 - prefixLength);
        int value = mask;
        byte[] bytes = new byte[]{
                (byte) (value >>> 24),
                (byte) (value >> 16 & 0xff),
                (byte) (value >> 8 & 0xff),
                (byte) (value & 0xff)
        };
        try {
            InetAddress netAddr = InetAddress.getByAddress(bytes);
            return netAddr.getHostAddress();
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
}

