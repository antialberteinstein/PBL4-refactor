package controller;

import model.entity.Computer;
import model.repository.ComputerRepository;

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
//                              SERVLET: AgentListServlet                        //
// ============================================================================== //

/**
 * Servlet for retrieving list of all agents/computers.
 * 
 * Endpoint: /api/agents
 * Method: GET
 * Response: JSON array of agent objects
 */
@WebServlet("/api/agents")
public class AgentListServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    
    private ComputerRepository computerRepository;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        super.init();
        
        // Get shared repository from ServletContext
        this.computerRepository = (ComputerRepository) getServletContext().getAttribute("computerRepository");
        if (this.computerRepository == null) {
            throw new ServletException("ComputerRepository not initialized in ServletContext");
        }
        
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
            // Get all computers directly from database repository
            List<Computer> computers = computerRepository.getAllComputers();
            
            // Serialize to JSON and write response
            response.setStatus(HttpServletResponse.SC_OK);
            out.write(gson.toJson(computers));
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\": \"Failed to retrieve agents: " + e.getMessage() + "\"}");
        }
    }
}
