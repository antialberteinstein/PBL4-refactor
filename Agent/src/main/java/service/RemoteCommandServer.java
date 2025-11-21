package service;

import config.AppConfig;
import util.Logger;
import util.ProtocolManager;

import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * RemoteCommandServer - TCP server for receiving remote commands from Manager
 * 
 * Supports commands:
 * 1. Kill process by PID
 * 2. Shutdown computer
 * 3. Send warning to user
 * 4. Send message to user
 */
public class RemoteCommandServer extends Thread {

    private static final String COMPONENT = "RemoteCommandServer";

    private final AppConfig appConfig;
    private final ProtocolManager protocolManager;
    private CommandNotificationListener notificationListener;
    private ServerSocket serverSocket;
    private boolean running = true;

    public RemoteCommandServer(AppConfig appConfig, ProtocolManager protocolManager) {
        this.appConfig = appConfig;
        this.protocolManager = protocolManager;
    }
    
    /**
     * Set notification listener for GUI/CLI notifications
     */
    public void setNotificationListener(CommandNotificationListener listener) {
        this.notificationListener = listener;
    }

    /**
     * Start TCP server
     */
    public void open() throws Exception {
        if (serverSocket == null || serverSocket.isClosed()) {
            serverSocket = new ServerSocket(appConfig.AGENT_COMMAND_PORT);
            Logger.info(COMPONENT, "TCP server opened on port " + appConfig.AGENT_COMMAND_PORT);
        }
    }

    /**
     * Stop TCP server
     */
    public void close() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                Logger.info(COMPONENT, "TCP server closed");
            }
        } catch (IOException e) {
            Logger.error(COMPONENT, "Error closing server: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        Logger.info(COMPONENT, "Started listening for remote commands");

        while (running) {
            try {
                // Accept client connection
                Socket clientSocket = serverSocket.accept();
                Logger.debug(COMPONENT, "Client connected: " + clientSocket.getInetAddress());

                // Handle client in separate thread
                new Thread(() -> handleClient(clientSocket)).start();

            } catch (IOException e) {
                if (running) {
                    Logger.error(COMPONENT, "Error accepting connection: " + e.getMessage());
                }
            }
        }

        close();
        Logger.info(COMPONENT, "Stopped");
    }

    /**
     * Handle client request
     */
    private void handleClient(Socket clientSocket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            // Read request
            String request = in.readLine();
            if (request == null || request.isEmpty()) {
                Logger.debug(COMPONENT, "Empty request received");
                return;
            }

            Logger.debug(COMPONENT, "Received request: " + request);

            // Process request and send response
            String response = processRequest(request);
            out.println(response);
            Logger.debug(COMPONENT, "Sent response: " + response);

        } catch (IOException e) {
            Logger.error(COMPONENT, "Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                Logger.error(COMPONENT, "Error closing client socket: " + e.getMessage());
            }
        }
    }

    /**
     * Process incoming request and generate response
     */
    private String processRequest(String request) {
        // KILL_PROCESS|PID:1234
        if (request.startsWith(protocolManager.KILL_PROCESS_REQUEST)) {
            return handleKillProcess(request);
        }
        
        // SHUTDOWN
        else if (request.startsWith(protocolManager.SHUTDOWN_REQUEST)) {
            return handleShutdown(request);
        }
        
        // SEND_WARNING|MESSAGE:Your warning message
        else if (request.startsWith(protocolManager.SEND_WARNING_REQUEST)) {
            return handleSendWarning(request);
        }
        
        // SEND_MESSAGE|MESSAGE:Your message
        else if (request.startsWith(protocolManager.SEND_MESSAGE_REQUEST)) {
            return handleSendMessage(request);
        }
        
        else {
            return "ERROR|Unknown command";
        }
    }

    /**
     * Kill process by PID
     * Request format: <NHOM3>KILL_PROCESS|PID:1234
     */
    private String handleKillProcess(String request) {
        try {
            // Parse PID from request
            String[] parts = request.split("\\" + protocolManager.SEPARATOR);
            if (parts.length < 2) {
                return protocolManager.KILL_PROCESS_RESPONSE + protocolManager.SEPARATOR + "ERROR|Invalid format";
            }

            String pidPart = parts[1];
            if (!pidPart.startsWith("PID:")) {
                return protocolManager.KILL_PROCESS_RESPONSE + protocolManager.SEPARATOR + "ERROR|PID not found";
            }

            int pid = Integer.parseInt(pidPart.substring(4));
            Logger.info(COMPONENT, "Attempting to kill process with PID: " + pid);

            // Kill process based on OS
            boolean success = killProcess(pid);

            if (success) {
                String message = "Successfully killed process: " + pid;
                Logger.info(COMPONENT, message);
                
                // Show notification
                if (notificationListener != null) {
                    notificationListener.notifyKillProcess(pid, true);
                } else {
                    // CLI mode - print to console
                    System.out.println("\n[REMOTE COMMAND] " + message);
                }
                
                return protocolManager.KILL_PROCESS_RESPONSE + protocolManager.SEPARATOR + "SUCCESS|Process " + pid + " killed";
            } else {
                String message = "Failed to kill process: " + pid;
                Logger.error(COMPONENT, message);
                
                // Show notification
                if (notificationListener != null) {
                    notificationListener.notifyKillProcess(pid, false);
                } else {
                    // CLI mode - print to console
                    System.err.println("\n[REMOTE COMMAND ERROR] " + message);
                }
                
                return protocolManager.KILL_PROCESS_RESPONSE + protocolManager.SEPARATOR + "ERROR|Failed to kill process";
            }

        } catch (NumberFormatException e) {
            return protocolManager.KILL_PROCESS_RESPONSE + protocolManager.SEPARATOR + "ERROR|Invalid PID format";
        } catch (Exception e) {
            Logger.error(COMPONENT, "Error killing process: " + e.getMessage());
            return protocolManager.KILL_PROCESS_RESPONSE + protocolManager.SEPARATOR + "ERROR|" + e.getMessage();
        }
    }

    /**
     * Kill process by PID using OS-specific command
     */
    private boolean killProcess(int pid) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            Process process;

            if (os.contains("win")) {
                // Windows: taskkill /F /PID <pid>
                process = Runtime.getRuntime().exec("taskkill /F /PID " + pid);
            } else {
                // Unix/Linux/Mac: kill -9 <pid>
                process = Runtime.getRuntime().exec("kill -9 " + pid);
            }

            int exitCode = process.waitFor();
            return exitCode == 0;

        } catch (Exception e) {
            Logger.error(COMPONENT, "Error executing kill command: " + e.getMessage());
            return false;
        }
    }

    /**
     * Shutdown computer
     * Request format: <NHOM3>SHUTDOWN|DELAY:60
     */
    private String handleShutdown(String request) {
        try {
            // Parse delay (in seconds) from request, default to 60 seconds
            int delay = 60;
            String[] parts = request.split("\\" + protocolManager.SEPARATOR);
            if (parts.length >= 2 && parts[1].startsWith("DELAY:")) {
                delay = Integer.parseInt(parts[1].substring(6));
            }

            Logger.info(COMPONENT, "Attempting to shutdown computer in " + delay + " seconds");
            
            // Show notification
            if (notificationListener != null) {
                notificationListener.notifyShutdown(delay);
            } else {
                // CLI mode - print to console
                System.out.println("\n[REMOTE COMMAND] Computer will shutdown in " + delay + " seconds!");
            }

            // Execute shutdown command in separate thread
            final int shutdownDelay = delay;
            new Thread(() -> {
                try {
                    shutdownComputer(shutdownDelay);
                } catch (Exception e) {
                    Logger.error(COMPONENT, "Error during shutdown: " + e.getMessage());
                }
            }).start();

            return protocolManager.SHUTDOWN_RESPONSE + protocolManager.SEPARATOR + "SUCCESS|Shutdown scheduled in " + delay + " seconds";

        } catch (NumberFormatException e) {
            return protocolManager.SHUTDOWN_RESPONSE + protocolManager.SEPARATOR + "ERROR|Invalid delay format";
        } catch (Exception e) {
            Logger.error(COMPONENT, "Error scheduling shutdown: " + e.getMessage());
            return protocolManager.SHUTDOWN_RESPONSE + protocolManager.SEPARATOR + "ERROR|" + e.getMessage();
        }
    }

    /**
     * Execute shutdown command based on OS
     */
    private void shutdownComputer(int delay) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        String command;

        if (os.contains("win")) {
            // Windows: shutdown /s /t <seconds>
            command = "shutdown /s /t " + delay;
        } else if (os.contains("mac")) {
            // macOS: sudo shutdown -h +<minutes>
            int minutes = Math.max(1, delay / 60);
            command = "sudo shutdown -h +" + minutes;
        } else {
            // Linux: shutdown -h +<minutes>
            int minutes = Math.max(1, delay / 60);
            command = "shutdown -h +" + minutes;
        }

        Runtime.getRuntime().exec(command);
        Logger.info(COMPONENT, "Shutdown command executed: " + command);
    }

    /**
     * Send warning to user (dialog with warning icon)
     * Request format: <NHOM3>SEND_WARNING|MESSAGE:Your warning message
     */
    private String handleSendWarning(String request) {
        try {
            // Parse message from request
            String message = parseMessage(request);
            if (message == null) {
                return protocolManager.SEND_WARNING_RESPONSE + protocolManager.SEPARATOR + "ERROR|Invalid format";
            }

            Logger.info(COMPONENT, "Sending warning: " + message);

            // Show warning dialog or print to console
            if (notificationListener != null) {
                notificationListener.showNotification("Warning from Manager", message, "WARNING");
            } else {
                // CLI mode - print to console
                System.out.println("\n" + "=".repeat(60));
                System.out.println("[WARNING FROM MANAGER]");
                System.out.println(message);
                System.out.println("=".repeat(60) + "\n");
            }
            
            // Also show Swing dialog (works in both modes if GUI libraries available)
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                    null,
                    message,
                    "Warning from Manager",
                    JOptionPane.WARNING_MESSAGE
                );
            });

            return protocolManager.SEND_WARNING_RESPONSE + protocolManager.SEPARATOR + "SUCCESS|Warning sent";

        } catch (Exception e) {
            Logger.error(COMPONENT, "Error sending warning: " + e.getMessage());
            return protocolManager.SEND_WARNING_RESPONSE + protocolManager.SEPARATOR + "ERROR|" + e.getMessage();
        }
    }

    /**
     * Send message to user (dialog with information icon)
     * Request format: <NHOM3>SEND_MESSAGE|MESSAGE:Your message
     */
    private String handleSendMessage(String request) {
        try {
            // Parse message from request
            String message = parseMessage(request);
            if (message == null) {
                return protocolManager.SEND_MESSAGE_RESPONSE + protocolManager.SEPARATOR + "ERROR|Invalid format";
            }

            Logger.info(COMPONENT, "Sending message: " + message);

            // Show message dialog or print to console
            if (notificationListener != null) {
                notificationListener.showNotification("Message from Manager", message, "INFO");
            } else {
                // CLI mode - print to console
                System.out.println("\n" + "=".repeat(60));
                System.out.println("[MESSAGE FROM MANAGER]");
                System.out.println(message);
                System.out.println("=".repeat(60) + "\n");
            }
            
            // Also show Swing dialog (works in both modes if GUI libraries available)
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                    null,
                    message,
                    "Message from Manager",
                    JOptionPane.INFORMATION_MESSAGE
                );
            });

            return protocolManager.SEND_MESSAGE_RESPONSE + protocolManager.SEPARATOR + "SUCCESS|Message sent";

        } catch (Exception e) {
            Logger.error(COMPONENT, "Error sending message: " + e.getMessage());
            return protocolManager.SEND_MESSAGE_RESPONSE + protocolManager.SEPARATOR + "ERROR|" + e.getMessage();
        }
    }

    /**
     * Parse message content from request
     */
    private String parseMessage(String request) {
        String[] parts = request.split("\\" + protocolManager.SEPARATOR, 2);
        if (parts.length < 2) {
            return null;
        }

        String messagePart = parts[1];
        if (!messagePart.startsWith("MESSAGE:")) {
            return null;
        }

        return messagePart.substring(8);
    }
}
