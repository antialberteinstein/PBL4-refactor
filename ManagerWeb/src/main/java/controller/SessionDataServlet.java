package controller;

import model.entity.Session;
import model.repository.SessionRepository;

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
//                           SERVLET: SessionDataServlet                          //
// ============================================================================== //

/**
 * Servlet for retrieving session data for charts.
 * 
 * Supports:
 * - Get all sessions for an agent
 * - Get recent N sessions for an agent
 * - Get latest session for real-time update
 * 
 * Endpoint: /api/sessions
 * Method: GET
 * Parameters:
 *   - mac (required): MAC address of the agent
 *   - limit (optional): Number of sessions to return
 *   - latest (optional): If "true", return only latest session
 *   - startTime (optional): Start timestamp in milliseconds
 *   - endTime (optional): End timestamp in milliseconds
 * 
 * Response: JSON array of session objects or single session object
 */
@WebServlet("/api/sessions")
public class SessionDataServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    
    private SessionRepository sessionRepository;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        super.init();
        
        // Get shared repository from ServletContext
        this.sessionRepository = (SessionRepository) getServletContext().getAttribute("sessionRepository");
        if (this.sessionRepository == null) {
            throw new ServletException("SessionRepository not initialized in ServletContext");
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
            // 1. Get MAC address parameter (required)
            String macAddress = request.getParameter("mac");
            if (macAddress == null || macAddress.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("{\"error\": \"MAC address parameter is required\"}");
                return;
            }
            
            // 2. Get limit parameter (optional, default 100)
            String limitParam = request.getParameter("limit");
            int limit = 100; // default limit
            
            if (limitParam != null && !limitParam.trim().isEmpty()) {
                try {
                    limit = Integer.parseInt(limitParam);
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.write("{\"error\": \"Invalid limit parameter\"}");
                    return;
                }
            }
            
            // 3. Check if requesting latest session only
            String latestParam = request.getParameter("latest");
            if ("true".equalsIgnoreCase(latestParam)) {
                List<Session> sessions = sessionRepository.getSessionsByMac(macAddress, 1);
                if (sessions != null && !sessions.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    out.write(gson.toJson(sessions.get(0)));
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.write("{\"error\": \"No sessions found for this agent\"}");
                }
                return;
            }
            
            // 4. Get time range parameters (optional)
            String startTimeParam = request.getParameter("startTime");
            String endTimeParam = request.getParameter("endTime");
            
            List<Session> sessions;
            
            if (startTimeParam != null && endTimeParam != null) {
                // Time range query
                try {
                    long startTime = Long.parseLong(startTimeParam);
                    long endTime = Long.parseLong(endTimeParam);
                    
                    sessions = sessionRepository.getSessionsByMac(macAddress, limit);
                    
                    // Filter sessions by time range
                    sessions = sessions.stream()
                        .filter(s -> s.getTimestamp() >= startTime && s.getTimestamp() <= endTime)
                        .limit(limit)
                        .collect(java.util.stream.Collectors.toList());
                        
                } catch (NumberFormatException e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.write("{\"error\": \"Invalid time parameters\"}");
                    return;
                }
            } else {
                // Regular query with limit
                sessions = sessionRepository.getSessionsByMac(macAddress, limit);
            }
            
            // 5. Return sessions
            response.setStatus(HttpServletResponse.SC_OK);
            out.write(gson.toJson(sessions));
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\": \"Failed to retrieve sessions: " + e.getMessage() + "\"}");
        }
    }
}
