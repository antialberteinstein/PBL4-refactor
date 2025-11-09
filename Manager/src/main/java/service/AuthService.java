package service;

import database.AuthRepository;
import model.User;
import util.Logger;

/**
 * Authentication Service.
 * 
 * Provides high-level authentication operations for the Manager system.
 * Handles user login and session management.
 * 
 * Design Pattern: Service Layer (business logic)
 */
public class AuthService {
    
    private static final String COMPONENT = "AuthService";
    
    // ============================================================================== //
    //                                   DEPENDENCIES                                 //
    // ============================================================================== //
    
    private final AuthRepository authRepository;
    private User currentUser; // Currently logged-in user
    
    
    // ============================================================================== //
    //                                  CONSTRUCTOR                                   //
    // ============================================================================== //
    
    /**
     * Constructor with Dependency Injection.
     * 
     * @param authRepository Authentication repository
     */
    public AuthService(AuthRepository authRepository) {
        this.authRepository = authRepository;
        this.currentUser = null;
    }
    
    
    // ============================================================================== //
    //                            AUTHENTICATION OPERATIONS                           //
    // ============================================================================== //
    
    /**
     * Login with username and password.
     * 
     * Workflow:
     * ### Step 1: Validate inputs
     * ### Step 2: Authenticate via repository
     * ### Step 3: Store current user if successful
     * ### Step 4: Return result
     * 
     * @param username User's login name
     * @param password User's password
     * @return true if login successful, false otherwise
     */
    public boolean login(
        String username, 
        String password
    ) {
        // ### Step 1: Validate inputs
        if (username == null || username.trim().isEmpty()) {
            Logger.warn(COMPONENT, "Login failed: username is empty");
            return false;
        }
        
        if (password == null || password.isEmpty()) {
            Logger.warn(COMPONENT, "Login failed: password is empty");
            return false;
        }
        
        // ### Step 2: Authenticate
        User user = authRepository.authenticate(username.trim(), password);
        
        // ### Step 3: Store current user
        if (user != null) {
            this.currentUser = user;
            Logger.success(COMPONENT, "User '" + username + "' logged in successfully");
            return true;
        } else {
            Logger.warn(COMPONENT, "Login failed for user '" + username + "' - invalid credentials");
            return false;
        }
    }
    
    /**
     * Logout current user.
     */
    public void logout() {
        if (currentUser != null) {
            Logger.info(COMPONENT, "User '" + currentUser.getUsername() + "' logged out");
            currentUser = null;
        }
    }
    
    /**
     * Check if a user is currently logged in.
     * 
     * @return true if user is logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Get currently logged-in user.
     * 
     * @return Current User object, or null if not logged in
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Initialize default admin account.
     * 
     * Creates admin/admin if no users exist in database.
     */
    public void initializeDefaultAdmin() {
        authRepository.initializeDefaultAdmin();
    }
}
