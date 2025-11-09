package util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * ExternalScanClient - Simple TCP client for testing ExternalScanServer
 * 
 * This utility class provides a simple way to send scan requests to the
 * ExternalScanServer. It can be used for testing or as an example for
 * external applications that need to trigger scans.
 * 
 * Usage:
 *   java util.ExternalScanClient <host> <port>
 *   java util.ExternalScanClient localhost 8888
 * 
 * Protocol:
 * - Connects to server via TCP
 * - Sends "SCAN\n" command
 * - Reads response line
 * - Closes connection
 */
public class ExternalScanClient {
    
    /**
     * Send a scan request to the ExternalScanServer
     * 
     * @param host Hostname or IP address of the Manager
     * @param port TCP port of the ExternalScanServer
     * @return Server response string
     * @throws Exception If connection or communication fails
     */
    public static String sendScanRequest(String host, int port) throws Exception {
        // ### 1. Connect to server
        try (Socket socket = new Socket(host, port)) {
            
            // ### 2. Create streams
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
            
            // ### 3. Send SCAN command
            out.println("SCAN");
            
            // ### 4. Read response
            String response = in.readLine();
            
            // ### 5. Return response
            return response;
        }
    }
    
    /**
     * Command-line interface for testing
     * 
     * Usage:
     *   java util.ExternalScanClient <host> <port>
     * 
     * Examples:
     *   java util.ExternalScanClient localhost 8888
     *   java util.ExternalScanClient 192.168.1.100 8888
     */
    public static void main(String[] args) {
        // ### 1. Parse command-line arguments
        if (args.length != 2) {
            System.err.println("Usage: java util.ExternalScanClient <host> <port>");
            System.err.println();
            System.err.println("Examples:");
            System.err.println("  java util.ExternalScanClient localhost 8888");
            System.err.println("  java util.ExternalScanClient 192.168.1.100 8888");
            System.exit(1);
        }
        
        String host = args[0];
        int port;
        
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Error: Port must be a number");
            System.exit(1);
            return;
        }
        
        // ### 2. Send scan request
        System.out.println("Connecting to " + host + ":" + port + "...");
        
        try {
            String response = sendScanRequest(host, port);
            
            // ### 3. Display response
            System.out.println("Server response: " + response);
            
            // ### 4. Check if successful
            if ("OK".equals(response)) {
                System.out.println("✓ Scan triggered successfully!");
                System.exit(0);
            } else {
                System.err.println("✗ Scan failed: " + response);
                System.exit(1);
            }
            
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
