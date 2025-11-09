package controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Authentication Filter.
 * 
 * Intercepts all requests and checks if user is logged in.
 * Redirects to login page if not authenticated.
 * 
 * Excludes login-related URLs from filtering.
 */
@WebFilter("/*")
public class AuthFilter implements Filter {
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }
    
    @Override
    public void doFilter(
        ServletRequest request, 
        ServletResponse response,
        FilterChain chain
    ) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        
        // URLs that don't require authentication
        boolean isLoginPage = requestURI.equals(contextPath + "/login");
        boolean isLoginServlet = requestURI.equals(contextPath + "/login");
        boolean isLogoutServlet = requestURI.equals(contextPath + "/logout");
        boolean isStaticResource = requestURI.startsWith(contextPath + "/css/") ||
                                   requestURI.startsWith(contextPath + "/js/") ||
                                   requestURI.startsWith(contextPath + "/images/");
        
        // Check if user is logged in
        HttpSession session = httpRequest.getSession(false);
        boolean isLoggedIn = (session != null && session.getAttribute("user") != null);
        
        // Allow access to login-related URLs and static resources
        if (isLoginPage || isLoginServlet || isLogoutServlet || isStaticResource) {
            chain.doFilter(request, response);
            return;
        }
        
        // Redirect to login if not authenticated
        if (!isLoggedIn) {
            httpResponse.sendRedirect(contextPath + "/login");
            return;
        }
        
        // User is authenticated - proceed
        chain.doFilter(request, response);
    }
    
    @Override
    public void destroy() {
        // No cleanup needed
    }
}
