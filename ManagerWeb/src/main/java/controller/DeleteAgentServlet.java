package controller;

import model.repository.ComputerRepository;
import model.repository.DatabaseRepository;
import util.Logger;
import util.Messages;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Servlet for deleting agents
 */
@WebServlet("/api/delete-agent")
public class DeleteAgentServlet extends HttpServlet {
    
    private static final String COMPONENT = "DeleteAgentServlet";
    private ComputerRepository computerRepository;
    
    @Override
    public void init() throws ServletException {
        super.init();
        // Get shared repository from ServletContext
        this.computerRepository = (ComputerRepository) getServletContext().getAttribute("computerRepository");
        if (this.computerRepository == null) {
            throw new ServletException("ComputerRepository not initialized in ServletContext");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        
        // Check authentication
        if (request.getSession().getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\": false, \"message\": \"" + Messages.get("auth.unauthorized") + "\"}");
            return;
        }
        
        String macAddress = request.getParameter("mac");
        
        if (macAddress == null || macAddress.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\": false, \"message\": \"Missing MAC address\"}");
            return;
        }
        
        try {
            boolean success = computerRepository.deleteComputer(macAddress);
            
            if (success) {
                Logger.info(COMPONENT, "Agent deleted successfully: " + macAddress);
                out.print("{\"success\": true, \"message\": \"Agent deleted successfully\"}");
            } else {
                Logger.warn(COMPONENT, "Failed to delete agent: " + macAddress);
                out.print("{\"success\": false, \"message\": \"Failed to delete agent. Agent may not exist.\"}");
            }
        } catch (Exception e) {
            Logger.error(COMPONENT, "Error deleting agent: " + macAddress, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"message\": \"Internal server error\"}");
        }
    }
}
