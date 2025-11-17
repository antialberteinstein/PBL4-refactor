package controller;

import config.AppConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.net.Socket;

/**
 * RemoteCommandServlet - Handles remote command requests from web interface
 * 
 * This servlet receives commands from the web UI and forwards them to the
 * Manager's ExternalScanServer via TCP connection.
 * 
 * Supported commands:
 * - KILL_PROCESS|MAC|PID
 * - SHUTDOWN|MAC|DELAY
 * - SEND_MESSAGE|MAC|MESSAGE
 */
@WebServlet("/remote-command")
public class RemoteCommandServlet extends HttpServlet {

    /**
     * Send command to Manager's ExternalScanServer
     * 
     * @param managerHost Manager host (localhost for same machine)
     * @param managerPort Manager ExternalScanServer port
     * @param command Command to send
     * @return Response from server
     */
    private String sendCommandToManager(String managerHost, int managerPort, String command) {
        try (Socket socket = new Socket(managerHost, managerPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            // Send command
            out.println(command);
            
            // Receive response
            String response = in.readLine();
            return response != null ? response : "ERROR: No response from server";
            
        } catch (IOException e) {
            System.err.println("Error sending command to Manager: " + e.getMessage());
            return "ERROR: Connection failed - " + e.getMessage();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Set response type
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        // Get parameters
        String commandType = request.getParameter("command");
        String macAddress = request.getParameter("mac");
        
        // Validate required parameters
        if (commandType == null || macAddress == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\": false, \"message\": \"Missing required parameters\"}");
            return;
        }
        
        // Build command based on type
        String command = null;
        
        switch (commandType) {
            case "KILL_PROCESS":
                String pid = request.getParameter("pid");
                if (pid == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"success\": false, \"message\": \"Missing PID parameter\"}");
                    return;
                }
                command = "KILL_PROCESS|" + macAddress + "|" + pid;
                break;
                
            case "SHUTDOWN":
                String delay = request.getParameter("delay");
                if (delay == null || delay.isEmpty()) {
                    delay = "60"; // Default 60 seconds
                }
                command = "SHUTDOWN|" + macAddress + "|" + delay;
                break;
                
            case "SEND_MESSAGE":
                String message = request.getParameter("message");
                if (message == null || message.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"success\": false, \"message\": \"Missing message parameter\"}");
                    return;
                }
                command = "SEND_MESSAGE|" + macAddress + "|" + message;
                break;
                
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"Unknown command type: " + commandType + "\"}");
                return;
        }
        
        // Get Manager configuration
        ServletContext context = getServletContext();
        AppConfig appConfig = (AppConfig) context.getAttribute("appConfig");
        
        if (appConfig == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\": false, \"message\": \"Server configuration not available\"}");
            return;
        }
        
        // Send command to Manager (assuming Manager runs on same machine)
        String managerHost = "localhost";
        int managerPort = appConfig.getExternalScanPort();
        
        System.out.println("Sending command to Manager: " + command);
        String serverResponse = sendCommandToManager(managerHost, managerPort, command);
        System.out.println("Manager response: " + serverResponse);
        
        // Parse response and return JSON
        boolean success = serverResponse != null && serverResponse.startsWith("OK");
        String resultMessage = serverResponse != null ? serverResponse : "No response from server";
        
        // Build JSON response
        String jsonResponse = String.format(
            "{\"success\": %s, \"message\": \"%s\"}",
            success,
            resultMessage.replace("\"", "\\\"")
        );
        
        response.getWriter().write(jsonResponse);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.getWriter().write("{\"success\": false, \"message\": \"Use POST method\"}");
    }
}
