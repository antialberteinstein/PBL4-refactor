package controller;

import model.entity.Process;
import model.repository.ProcessRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import com.google.gson.Gson;

// ============================================================================== //
//                           SERVLET: ProcessListServlet                          //
// ============================================================================== //

/**
 * Servlet for retrieving process list for a specific session.
 * 
 * Endpoint: /api/processes
 * Method: GET
 * Parameters:
 *   - sessionId (required): Session ID to get processes for
 * 
 * Response: JSON array of process objects
 */
@WebServlet("/api/processes")
public class ProcessListServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    
    private ProcessRepository processRepository;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        super.init();
        
        // Get shared repository from ServletContext
        this.processRepository = (ProcessRepository) getServletContext().getAttribute("processRepository");
        if (this.processRepository == null) {
            throw new ServletException("ProcessRepository not initialized in ServletContext");
        }
        
        // Initialize GSON for JSON serialization
        gson = new Gson();
    }
    
    @Override
    protected void doGet(
        HttpServletRequest request, 
        HttpServletResponse response
    ) throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        
        try {
            // Get session ID parameter (required)
            String sessionIdParam = request.getParameter("sessionId");
            if (sessionIdParam == null || sessionIdParam.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("{\"error\": \"Session ID parameter is required\"}");
                return;
            }
            
            // Parse session ID
            int sessionId;
            try {
                sessionId = Integer.parseInt(sessionIdParam);
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("{\"error\": \"Invalid session ID\"}");
                return;
            }
            
            // Get processes for session
            List<Process> processes = processRepository.getProcessesBySessionId(sessionId);
            
            // Return processes
            response.setStatus(HttpServletResponse.SC_OK);
            out.write(gson.toJson(processes));
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\": \"Failed to retrieve processes: " + e.getMessage() + "\"}");
        }
    }
}
