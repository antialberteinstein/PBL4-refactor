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
        String cpuThresholdStr = request.getParameter("cpuThreshold");
        String ramThresholdStr = request.getParameter("ramThreshold");
        
        boolean validInput = true;
        double cpuThreshold = 90.0;
        double ramThreshold = 90.0;
        
        // Validate threshold inputs
        if (cpuThresholdStr != null && !cpuThresholdStr.isEmpty()) {
            try {
                cpuThreshold = Double.parseDouble(cpuThresholdStr);
                if (cpuThreshold < 0 || cpuThreshold > 100) {
                    validInput = false;
                }
            } catch (NumberFormatException e) {
                validInput = false;
            }
        }
        
        if (ramThresholdStr != null && !ramThresholdStr.isEmpty()) {
            try {
                ramThreshold = Double.parseDouble(ramThresholdStr);
                if (ramThreshold < 0 || ramThreshold > 100) {
                    validInput = false;
                }
            } catch (NumberFormatException e) {
                validInput = false;
            }
        }
        
        if (validInput && language != null && (language.equals("en") || language.equals("vi"))) {
            // Store language preference in session
            session.setAttribute("language", language);
            
            // Update Messages language
            Messages.setLanguage(language);
            
            // Save to config.json (shared with Manager GUI)
            try {
                AppConfig appConfig = (AppConfig) context.getAttribute("appConfig");
                if (appConfig != null) {
                    appConfig.setLanguage(language);
                    appConfig.setCpuThresholdPercent(cpuThreshold);
                    appConfig.setRamThresholdPercent(ramThreshold);
                    ConfigManager.saveConfig(appConfig);
                    System.out.println("✓ Settings saved to config.json:");
                    System.out.println("  - Language: " + language);
                    System.out.println("  - CPU Threshold: " + cpuThreshold + "%");
                    System.out.println("  - RAM Threshold: " + ramThreshold + "%");
                }
            } catch (Exception e) {
                System.err.println("Error saving configuration: " + e.getMessage());
                e.printStackTrace();
            }
            
            // Redirect back to settings page with success message
            response.sendRedirect(request.getContextPath() + "/settings.jsp?saved=true");
        } else {
            // Invalid input, redirect back with error
            response.sendRedirect(request.getContextPath() + "/settings.jsp?error=true");
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // Redirect GET requests to settings page
        response.sendRedirect(request.getContextPath() + "/settings.jsp");
    }
}
