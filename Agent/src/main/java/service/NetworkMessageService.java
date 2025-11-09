package service;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

import java.util.ArrayList;
import java.util.List;

import util.Logger;
import config.AppConfig;

/**
 * NetworkMessageService - UDP-based communication service
 * 
 * Responsibilities:
 * 1. Listen for incoming UDP messages from Manager
 * 2. Route messages to appropriate monitors based on message type
 * 3. Send responses back to Manager
 * 4. Buffer unprocessed messages for later retrieval
 * 
 * Architecture Pattern: Chain of Responsibility
 * - Each monitor checks if message matches its protocol
 * - If matched, monitor generates response and message is not buffered
 * - If no monitor matches, message is added to buffer
 * 
 * Threading: Runs as a separate thread to continuously listen for messages
 */
public class NetworkMessageService extends Thread {

    private static final String COMPONENT = "NetworkMessageService";

    private DatagramSocket mailbox;                      // UDP socket for send/receive
    private AppConfig appConfig;                         // Configuration (port number)
    private List<String> buffer;                         // Unprocessed messages
    private ScanningMonitor scanningMonitor;             // Handles HELLO_REQUEST
    private ComputerSendingMonitor computerSendingMonitor; // Handles GET_COMPUTER_INFO_REQUEST
    private SessionSendingMonitor sessionSendingMonitor;   // Handles GET_SESSION_REQUEST

    private boolean running = true;

    /**
     * Constructor - Inject all dependencies
     * 
     * @param appConfig Configuration with UDP port
     * @param scanningMonitor Handles discovery/scanning requests
     * @param computerSendingMonitor Handles computer info requests
     * @param sessionSendingMonitor Handles session data requests
     */
    public NetworkMessageService(AppConfig appConfig, ScanningMonitor scanningMonitor, 
                                 ComputerSendingMonitor computerSendingMonitor, 
                                 SessionSendingMonitor sessionSendingMonitor) {
        this.appConfig = appConfig;
        this.buffer = new ArrayList<>();
        this.scanningMonitor = scanningMonitor;
        this.computerSendingMonitor = computerSendingMonitor;
        this.sessionSendingMonitor = sessionSendingMonitor;
    }

    /**
     * Open UDP socket on configured port
     * Must be called before sending/receiving messages
     */
    public void open() throws Exception {
        if (mailbox == null || mailbox.isClosed()) {
            mailbox = new DatagramSocket(appConfig.AGENT_UDP_PORT);
        }
    }

    /**
     * Close UDP socket and release resources
     */
    public void close() {
        if (mailbox != null && !mailbox.isClosed()) {
            mailbox.close();
        }
    }

    /**
     * Send UDP message to specified destination
     * 
     * @param message Message content (string)
     * @param ipAddress Destination IP address
     * @param port Destination port
     * @throws Exception if socket is not open or send fails
     */
    public void sendMessage(String message, String ipAddress, int port) throws Exception {
        if (mailbox == null || mailbox.isClosed()) {
            throw new IllegalStateException("Socket is not open. Call open() before sending messages.");
        }
        
        InetAddress receiverAddress = InetAddress.getByName(ipAddress);
        byte[] buffer = message.getBytes();
        DatagramPacket mail = new DatagramPacket(buffer, buffer.length, receiverAddress, port);
        mailbox.send(mail);
    }

    /**
     * Receive UDP message (blocking call)
     * 
     * @return Received datagram packet
     * @throws Exception if socket is not open or receive fails
     */
    public DatagramPacket receiveMessage() throws Exception {
        if (mailbox == null || mailbox.isClosed()) {
            throw new IllegalStateException("Socket is not open. Call open() before receiving messages.");
        }

        byte[] buffer = new byte[1024];
        DatagramPacket mail = new DatagramPacket(buffer, buffer.length);

        mailbox.receive(mail);  // Blocks until message arrives
        return mail;
    }

    /**
     * Main message processing loop (runs in separate thread)
     * 
     * Processing flow:
     * ### Step 1: Receive incoming UDP message
     * ### Step 2: Check if scanning monitor can handle it
     * ### Step 3: Check if computer monitor can handle it
     * ### Step 4: Check if session monitor can handle it
     * ### Step 5: If no monitor handled it, add to buffer
     * 
     * Uses Chain of Responsibility pattern - first matching monitor handles the message
     */
    public void run() {
        while (running) {
            try {
                // ### Step 1: Receive UDP packet
                DatagramPacket mail = receiveMessage();
                String message = new String(mail.getData(), 0, mail.getLength());
                
                // ### Step 2: Try scanning monitor (handles HELLO_REQUEST)
                String response = scanningMonitor.checkMessage(message);
                if (response != null) {
                    sendMessage(response, mail.getAddress().getHostAddress(), mail.getPort());
                    continue; // Message handled, skip buffer
                }

                // ### Step 3: Try computer monitor (handles GET_COMPUTER_INFO_REQUEST)
                response = computerSendingMonitor.checkMessage(message);
                if (response != null) {
                    sendMessage(response, mail.getAddress().getHostAddress(), mail.getPort());
                    continue; // Message handled, skip buffer
                }

                // ### Step 4: Try session monitor (handles GET_SESSION_REQUEST)
                List<String> responses = sessionSendingMonitor.checkMessage(message);
                if (responses != null) {
                    // May return multiple messages (timestamp + sessions + processes)
                    for (String resp : responses) {
                        sendMessage(resp, mail.getAddress().getHostAddress(), mail.getPort());
                    }
                    continue; // Message handled, skip buffer
                }

                // ### Step 5: No monitor handled it - add to buffer for manual processing
                buffer.add(message);

            } catch (Exception e) {
                e.printStackTrace();
                break; // Exit loop on error
            }
        }

        this.close();
    }

    public void stopNow() {
        running = false;
    }

    /**
     * Find and remove messages from buffer by prefix
     * Thread-safe method for retrieving buffered messages
     * 
     * @param prefix Message prefix to search for
     * @return List of matching messages (removed from buffer)
     */
    public synchronized List<String> findMailsWithPrefix(String prefix) {
        List<String> relevantMails = new ArrayList<>();
        
        // Find matching messages
        for (String mail : buffer) {
            if (mail.startsWith(prefix)) {
                relevantMails.add(mail);
            }
        }

        // Remove found messages from buffer
        for (String mail : relevantMails) {
            buffer.remove(mail);
        }

        Logger.debug(COMPONENT, "Found " + relevantMails.size() + " relevant mails with prefix: " + prefix);
        return relevantMails;
    }
}
