package service;

import config.AppConfig;
import util.Logger;
import util.ProtocolManager;

import java.io.*;
import java.net.Socket;

/**
 * RemoteCommandClient - TCP client for sending remote commands to Agent
 * 
 * Supports commands:
 * 1. Kill process by PID
 * 2. Shutdown computer
 * 3. Send warning to user
 * 4. Send message to user
 */
public class RemoteCommandClient {

    private static final String COMPONENT = "RemoteCommandClient";

    private final AppConfig appConfig;
    private final ProtocolManager protocolManager;

    public RemoteCommandClient(AppConfig appConfig, ProtocolManager protocolManager) {
        this.appConfig = appConfig;
        this.protocolManager = protocolManager;
    }

    /**
     * Send command to Agent and get response
     * 
     * @param agentIp IP address of the Agent
     * @param command Command to send
     * @return Response from Agent
     * @throws Exception If connection fails or error occurs
     */
    private String sendCommand(String agentIp, String command) throws Exception {
        Socket socket = null;
        try {
            // Connect to Agent TCP server
            socket = new Socket(agentIp, appConfig.getAgentTcpPort());
            
            // Send command
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(command);
            
            // Receive response
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String response = in.readLine();
            
            Logger.debug(COMPONENT, "Sent command to " + agentIp + ": " + command);
            Logger.debug(COMPONENT, "Received response: " + response);
            
            return response;
            
        } catch (Exception e) {
            Logger.error(COMPONENT, "Error sending command to " + agentIp + ": " + e.getMessage());
            throw new Exception("Failed to connect to Agent at " + agentIp + ": " + e.getMessage());
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    Logger.error(COMPONENT, "Error closing socket: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Kill process by PID on Agent
     * 
     * @param agentIp IP address of the Agent
     * @param pid Process ID to kill
     * @throws Exception If command fails
     */
    public void killProcess(String agentIp, int pid) throws Exception {
        String command = protocolManager.KILL_PROCESS_REQUEST + protocolManager.SEPARATOR + "PID:" + pid;
        String response = sendCommand(agentIp, command);
        
        if (response == null || !response.contains("SUCCESS")) {
            throw new Exception("Agent returned error: " + (response != null ? response : "No response"));
        }
        Logger.info(COMPONENT, "Successfully killed process " + pid + " on " + agentIp);
    }

    /**
     * Shutdown Agent computer
     * 
     * @param agentIp IP address of the Agent
     * @param delaySeconds Delay in seconds before shutdown (default 60)
     * @throws Exception If command fails
     */
    public void shutdown(String agentIp, int delaySeconds) throws Exception {
        String command = protocolManager.SHUTDOWN_REQUEST + protocolManager.SEPARATOR + "DELAY:" + delaySeconds;
        String response = sendCommand(agentIp, command);
        
        if (response == null || !response.contains("SUCCESS")) {
            throw new Exception("Agent returned error: " + (response != null ? response : "No response"));
        }
        Logger.info(COMPONENT, "Shutdown scheduled on " + agentIp + " in " + delaySeconds + " seconds");
    }

    /**
     * Send warning message to Agent user
     * 
     * @param agentIp IP address of the Agent
     * @param message Warning message to display
     * @throws Exception If command fails
     */
    public void sendWarning(String agentIp, String message) throws Exception {
        String command = protocolManager.SEND_WARNING_REQUEST + protocolManager.SEPARATOR + "MESSAGE:" + message;
        String response = sendCommand(agentIp, command);
        
        if (response == null || !response.contains("SUCCESS")) {
            throw new Exception("Agent returned error: " + (response != null ? response : "No response"));
        }
        Logger.info(COMPONENT, "Warning sent to " + agentIp);
    }

    /**
     * Send message to Agent user
     * 
     * @param agentIp IP address of the Agent
     * @param message Message to display
     * @throws Exception If command fails
     */
    public void sendMessage(String agentIp, String message) throws Exception {
        String command = protocolManager.SEND_MESSAGE_REQUEST + protocolManager.SEPARATOR + "MESSAGE:" + message;
        String response = sendCommand(agentIp, command);
        
        if (response == null || !response.contains("SUCCESS")) {
            throw new Exception("Agent returned error: " + (response != null ? response : "No response"));
        }
        Logger.info(COMPONENT, "Message sent to " + agentIp);
    }
}
