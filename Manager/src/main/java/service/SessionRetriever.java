package service;

import config.AppConfig;
import database.ComputerManager;
import database.ProcessManager;
import database.SessionManager;
import model.Computer;
import util.Logger;
import util.ProtocolManager;
import model.Session;
import model.Process;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class SessionRetriever {

    // Lop nay la mot thread dung de truy van session cua moi Computer.
    public static class SessionRequest extends Thread {
        public String ip;
        public int port;
        public String macAddress;

        private SessionRetriever retriever;

        private AppConfig appConfig;

        public SessionRequest(String ip, int port, String macAddress, SessionRetriever retriever, AppConfig appConfig) {
            this.ip = ip;
            this.port = port;
            this.macAddress = macAddress;
            this.retriever = retriever;
            this.appConfig = appConfig;
        }

        public void run() {
            while (!isInterrupted()) {
                try {
                    retriever.sendGetSessions(macAddress, ip, port);
                    Thread.sleep(appConfig.getSessionRetrievingDelayMs());
                } catch (InterruptedException e) {
                    // Thread interrupted, exit gracefully
                    break;
                } catch (Exception e) {
                    // Other exceptions, continue running
                }
            }
        }

    }

    public static class Postman {
        private NetworkMessageService postman;
        private ProtocolManager protocolManager;

        public Postman(NetworkMessageService postman, ProtocolManager protocolManager) {
            this.postman = postman;
            this.protocolManager = protocolManager;
        }

        // Lay thong tin may truoc khi lay session.
        public void requestComputerInfo(String agentIp, int port) throws Exception {
            String request = protocolManager.GET_COMPUTER_INFO_REQUEST;
            postman.sendMessage(request, agentIp, port);
        }

        public void requestSession(String agentIp, int port) throws Exception {
            String request = protocolManager.GET_SESSION_REQUEST;
            postman.sendMessage(request, agentIp, port);
        }
    }

    private final AppConfig appConfig;
    private final Postman postman;
    private final ProtocolManager protocolManager;
    private final NetworkMessageService networkMessageService;
    private final ComputerManager computerManager;
    private final SessionManager sessionManager;
    private final ProcessManager processManager;
    private AgentDiscoveryListener agentDiscoveryListener; // Callback for newly discovered Agents

    public SessionRetriever(AppConfig appConfig, NetworkMessageService networkMessageService, ProtocolManager protocolManager, 
                          ComputerManager computerManager, SessionManager sessionManager, ProcessManager processManager) {
        this.appConfig = appConfig;
        this.networkMessageService = networkMessageService;
        this.networkMessageService.setSessionRetriever(this);
        this.protocolManager = protocolManager;
        this.computerManager = computerManager;
        this.sessionManager = sessionManager;
        this.processManager = processManager;
        this.postman = new Postman(networkMessageService, protocolManager);
    }

    /**
     * Set callback listener for Agent discovery events
     * Follows Dependency Inversion Principle - depends on abstraction, not concrete class
     * 
     * @param listener Callback to notify when new Agent is discovered
     */
    public void setAgentDiscoveryListener(AgentDiscoveryListener listener) {
        this.agentDiscoveryListener = listener;
    }

    // Goi de gui tin nhan yeu cau lay thong tin may tinh.
    public void sendGetComputerInfoRequest(String ip, int port) throws Exception {
        this.postman.requestComputerInfo(ip, port);
    }

    public void sendGetSessions(String macAddress, String ip, int port) throws Exception {
        this.postman.requestSession(ip, port);
    }

    public String checkMessage(String message) {
        String prefixComputer = protocolManager.GET_COMPUTER_INFO_RESPONSE + protocolManager.SEPARATOR;
        String prefixSession = protocolManager.GET_SESSION_RESPONSE + protocolManager.SEPARATOR;
        String prefixProcess = protocolManager.PROCESS_RESPONSE + protocolManager.SEPARATOR;
        if (message.startsWith(prefixComputer)) {
            if (parseAndSaveComputerInfo(message.substring(prefixComputer.length()))) {
                Logger.debug("SessionRetriever", "Received Computer Info: " + message);
                return "COMPUTER_INFO_SAVED";
            } else {
                return null;
            }
        } else if (message.startsWith(prefixSession)) {
            if (parseAndSaveSession(message.substring(prefixSession.length()))) {
                Logger.debug("SessionRetriever", "Received Session Info: " + message);
                return "SESSION_SAVED";
            } else {
                return null;
            }
        } else if (message.startsWith(prefixProcess)) {
            if (parseAndSaveProcess(message.substring(prefixProcess.length()))) {
                Logger.debug("SessionRetriever", "Received Process Info: " + message);
                return "PROCESS_SAVED";
            } else {
                return null;
            }
        }
        return null;
    }

    // Agent co the gui mot process trong mot tin nhan.
    private boolean parseAndSaveProcess(String data) {
        String [] parts = data.split("\\" + protocolManager.SEPARATOR);

        String macPrefix = "MAC:";
        String timestampPrefix = "TIMESTAMP:";
        String processPidPrefix = "PROCESS_PID:";
        String processNamePrefix = "PROCESS_NAME:";
        String processCpuUsagePrefix = "PROCESS_CPU_USAGE:";
        String processRamUsagePrefix = "PROCESS_RAM_USAGE:";

        Process process = new Process();
        String macAddress = "";
        long timestamp = 0;

        for (String part : parts) {
            if (part.startsWith(macPrefix)) {
                macAddress = part.substring(macPrefix.length());
            } else if (part.startsWith(processPidPrefix)) {
                try {
                    process.setPid(Integer.parseInt(part.substring(processPidPrefix.length())));
                } catch (NumberFormatException e) {
                    process.setPid(0);
                }
            } else if (part.startsWith(timestampPrefix)) {
                timestamp = Long.parseLong(part.substring(timestampPrefix.length()));
            } else if (part.startsWith(processNamePrefix)) {
                process.setName(part.substring(processNamePrefix.length()));
            } else if (part.startsWith(processCpuUsagePrefix)) {
                try {
                    process.setCpuUsage(Double.parseDouble(part.substring(processCpuUsagePrefix.length())));
                } catch (NumberFormatException e) {
                    process.setCpuUsage(0.0);
                }
            } else if (part.startsWith(processRamUsagePrefix)) {
                try {
                    process.setRamUsage(Long.parseLong(part.substring(processRamUsagePrefix.length())));
                } catch (NumberFormatException e) {
                    process.setRamUsage(0L);
                }
            } else {
                // Có lỗi khi parse.
                // Có lỗi khi parse.
                return false;
            }
        }

        long id = sessionManager.getSessionIdByMacAndTimestamp(macAddress, timestamp);

        if (id == -1) {
            System.out.println("No matching session found for Process with MAC: " + macAddress + " and Timestamp: " + timestamp);
            return false;
        }

        process.setSessionId(id);

        // Lưu process vào danh sách.
        processManager.saveProcess(process);
        return true;
    }

    // Agent se gui mot session trong mot tin nhan.
    private boolean parseAndSaveSession(String data) {
        String[] parts = data.split("\\" + protocolManager.SEPARATOR);

        String macAddressPrefix = "MAC:";
        String cpuUsagePrefix = "CPU_USAGE:";
        String totalRamPrefix = "TOTAL_RAM:";
        String ramUsagePrefix = "RAM_USAGE:";
        String timestampPrefix = "TIMESTAMP:";

        Session session = new Session();

        for (String part : parts) {
            if (part.startsWith(macAddressPrefix)) {
                session.setMacAddress(part.substring(macAddressPrefix.length()));
            } else if (part.startsWith(cpuUsagePrefix)) {
                try {
                    session.setCpuUsage(Double.parseDouble(part.substring(cpuUsagePrefix.length())));
                } catch (NumberFormatException e) {
                    session.setCpuUsage(0.0);
                }
            } else if (part.startsWith(totalRamPrefix)) {
                try {
                    session.setTotalRam(Long.parseLong(part.substring(totalRamPrefix.length())));
                } catch (NumberFormatException e) {
                    session.setTotalRam(0L);
                }
            } else if (part.startsWith(ramUsagePrefix)) {
                try {
                    session.setRamUsage(Long.parseLong(part.substring(ramUsagePrefix.length())));
                } catch (NumberFormatException e) {
                    session.setRamUsage(0L);
                }
            } else if (part.startsWith(timestampPrefix)) {
                try {
                    session.setTimestamp(Long.parseLong(part.substring(timestampPrefix.length())));
                } catch (NumberFormatException e) {
                    session.setTimestamp(0L);
                }
            } else {
                // Có lỗi khi parse.
                return false;
            }
        }

        // Lưu session vào danh sách.
        sessionManager.saveSession(session);
        
        return true;
    }

        

    private boolean parseAndSaveComputerInfo(String data) {
        String[] parts = data.split("\\" + protocolManager.SEPARATOR);

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

        // Variables to store parsed values
        String hostname = null;
        String ipAddress = null;
        String macAddress = null;
        String os = null;
        String architecture = null;

        String manufacturer = null;
        String model = null;
        String serialNumber = null;

        String cpuName = null;
        String cpuVendor = null;
        int physicalCores = 0;
        int logicalCores = 0;
        long cpuMaxFreq = 0;

        for (String part : parts) {

            if (part.startsWith(hostNamePrefix)) {
                hostname = part.substring(hostNamePrefix.length());
            } else if (part.startsWith(ipAddressPrefix)) {
                ipAddress = part.substring(ipAddressPrefix.length());
            } else if (part.startsWith(macAddressPrefix)) {
                macAddress = part.substring(macAddressPrefix.length());
            } else if (part.startsWith(osPrefix)) {
                os = part.substring(osPrefix.length());
            } else if (part.startsWith(architecturePrefix)) {
                architecture = part.substring(architecturePrefix.length());
            } else if (part.startsWith(manufacturerPrefix)) {
                manufacturer = part.substring(manufacturerPrefix.length());
            } else if (part.startsWith(modelPrefix)) {
                model = part.substring(modelPrefix.length());
            } else if (part.startsWith(serialNumberPrefix)) {
                serialNumber = part.substring(serialNumberPrefix.length());
            } else if (part.startsWith(cpuNamePrefix)) {
                cpuName = part.substring(cpuNamePrefix.length());
            } else if (part.startsWith(cpuVendorPrefix)) {
                cpuVendor = part.substring(cpuVendorPrefix.length());
            } else if (part.startsWith(physicalCoresPrefix)) {
                try {
                    physicalCores = Integer.parseInt(part.substring(physicalCoresPrefix.length()));
                } catch (NumberFormatException e) {
                    physicalCores = 0;
                }
            } else if (part.startsWith(logicalCoresPrefix)) {
                try {
                    logicalCores = Integer.parseInt(part.substring(logicalCoresPrefix.length()));
                } catch (NumberFormatException e) {
                    logicalCores = 0;
                }
            } else if (part.startsWith(cpuMaxFreqPrefix)) {
                try {
                    cpuMaxFreq = Long.parseLong(part.substring(cpuMaxFreqPrefix.length()));
                } catch (NumberFormatException e) {
                    cpuMaxFreq = 0L;
                }
            } else {
                // Có lỗi khi parse.
                return false;
            }
        }


        Logger.debug("SessionRetriever", "Parsed Computer Info: " + hostname + " (" + macAddress + ")");

        // Luu du lieu vao database.
        Computer computer = new Computer();
        computer.setHostname(hostname);
        computer.setIpAddress(ipAddress);
        computer.setMacAddress(macAddress);
        computer.setOs(os);
        computer.setArchitecture(architecture);
        computer.setManufacturer(manufacturer);
        computer.setModel(model);
        computer.setSerialNumber(serialNumber);
        computer.setCpuName(cpuName);
        computer.setCpuVendor(cpuVendor);
        computer.setPhysicalCores(physicalCores);
        computer.setLogicalCores(logicalCores);
        computer.setCpuMaxFreq(cpuMaxFreq);

        boolean saved = computerManager.saveComputer(computer);
        
        // Notify listener about newly discovered Agent (if callback is set)
        if (saved && agentDiscoveryListener != null && macAddress != null && ipAddress != null) {
            agentDiscoveryListener.onAgentDiscovered(macAddress, ipAddress);
        }
        
        return saved;
    }
}
