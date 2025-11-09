package controller;

import config.AppConfig;
import model.repository.DatabaseRepository;
import model.repository.ComputerRepository;
import model.repository.SessionRepository;
import model.repository.ProcessRepository;
import service.WebCommandMode;
import service.WebCommandMode.CommandResult;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

// ============================================================================== //
//                                 SERVLET: ScanServlet                          //
// ============================================================================== //

/**
 * Servlet for triggering network scan.
 * 
 * Connects to running Manager's ExternalScanServer (TCP port 8888)
 * to request a network scan.
 * 
 * Endpoint: /api/scan
 * Method: POST
 * Response: JSON with success status and message
 */
@WebServlet("/api/scan")
public class ScanServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    
    // ============================================================================== //
    //                                   DEPENDENCIES                                 //
    // ============================================================================== //
    
    private WebCommandMode webCommandMode;
    private Gson gson;
    
    // ============================================================================== //
    //                                  INITIALIZATION                                //
    // ============================================================================== //
    
    @Override
    public void init() throws ServletException {
        super.init();
        
        // Get shared instance from ServletContext
        this.webCommandMode = (WebCommandMode) getServletContext().getAttribute("webCommandMode");
        if (this.webCommandMode == null) {
            throw new ServletException("WebCommandMode not initialized in ServletContext");
        }
        
        // Initialize GSON for JSON serialization
        gson = new Gson();
    }
    
    // ============================================================================== //
    //                                  HTTP METHODS                                  //
    // ============================================================================== //
    
    /**
     * Handle POST request to trigger scan.
     */
    @Override
    protected void doPost(
        HttpServletRequest request, 
        HttpServletResponse response
    ) throws ServletException, IOException {
        
        // Set response type to JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        
        try {
            // Execute scan command via TCP
            CommandResult result = webCommandMode.executeScan();
            
            // Prepare JSON response
            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("success", result.isSuccess());
            jsonResponse.put("message", result.getMessage());
            
            // Set HTTP status code
            if (result.isSuccess()) {
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
            
            // Write JSON response
            out.write(gson.toJson(jsonResponse));
            
        } catch (Exception e) {
            // Handle unexpected errors
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Internal server error: " + e.getMessage());
            
            out.write(gson.toJson(errorResponse));
        }
    }
    
    /**
     * Reject GET requests.
     */
    @Override
    protected void doGet(
        HttpServletRequest request, 
        HttpServletResponse response
    ) throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "Method not allowed. Use POST to trigger scan.");
        
        PrintWriter out = response.getWriter();
        out.write(gson.toJson(errorResponse));
    }
}
