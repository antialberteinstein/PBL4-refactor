package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import model.entity.User;
import model.repository.AuthRepository;
import util.Messages;

import java.io.IOException;

/**
 * LoginServlet - Handles user authentication for ManagerWeb.
 * 
 * Endpoints:
 * - GET /login - Show login page
 * - POST /login - Process login credentials
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    
    private AuthRepository authRepository;
    
    @Override
    public void init() throws ServletException {
        super.init();
        
        // Get AuthRepository from ServletContext
        this.authRepository = (AuthRepository) getServletContext().getAttribute("authRepository");
        if (this.authRepository == null) {
            throw new ServletException("AuthRepository not initialized in ServletContext");
        }
    }
    
    /**
     * Show login page.
     */
    @Override
    protected void doGet(
        HttpServletRequest request, 
        HttpServletResponse response
    ) throws ServletException, IOException {
        
        // Forward to login.jsp
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }
    
    /**
     * Process login credentials.
     */
    @Override
    protected void doPost(
        HttpServletRequest request, 
        HttpServletResponse response
    ) throws ServletException, IOException {
        
        // Get credentials from form
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        
        // Validate inputs
        if (username == null || username.trim().isEmpty() ||
            password == null || password.isEmpty()) {
            
            request.setAttribute("error", Messages.get("auth.invalid"));
            request.getRequestDispatcher("/login.jsp").forward(request, response);
            return;
        }
        
        // Authenticate
        User user = authRepository.authenticate(username.trim(), password);
        
        if (user != null) {
            // Login successful - create session
            HttpSession session = request.getSession();
            session.setAttribute("user", user);
            session.setAttribute("username", user.getUsername());
            session.setMaxInactiveInterval(3600); // 1 hour
            
            // Redirect to dashboard
            response.sendRedirect(request.getContextPath() + "/");
        } else {
            // Login failed - don't leak username information
            request.setAttribute("error", Messages.get("auth.invalid"));
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        }
    }
}
