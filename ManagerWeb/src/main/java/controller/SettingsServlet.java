package controller;

import config.AppConfig;
import config.ConfigManager;
import util.Messages;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Servlet for handling user settings
 */
@WebServlet("/settings")
public class SettingsServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession();
        ServletContext context = getServletContext();
        
        // Get language parameter
        String language = request.getParameter("language");
        
        if (language != null && (language.equals("en") || language.equals("vi"))) {
            // Store language preference in session
            session.setAttribute("language", language);
            
            // Update Messages language
            Messages.setLanguage(language);
            
            // Save to config.json (shared with Manager GUI)
            try {
                AppConfig appConfig = (AppConfig) context.getAttribute("appConfig");
                if (appConfig != null) {
                    appConfig.setLanguage(language);
                    ConfigManager.saveConfig(appConfig);
                    System.out.println("✓ Language saved to config.json: " + language);
                }
            } catch (Exception e) {
                System.err.println("Error saving configuration: " + e.getMessage());
                e.printStackTrace();
            }
            
            // Redirect back to settings page with success message
            response.sendRedirect(request.getContextPath() + "/settings.jsp?saved=true");
        } else {
            // Invalid language, redirect back
            response.sendRedirect(request.getContextPath() + "/settings.jsp");
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // Redirect GET requests to settings page
        response.sendRedirect(request.getContextPath() + "/settings.jsp");
    }
}
