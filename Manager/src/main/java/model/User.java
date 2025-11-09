package model;

import java.time.LocalDateTime;

/**
 * User entity for authentication.
 * 
 * Represents a user account with credentials for accessing the Manager system.
 * Passwords are stored as hashed values (SHA-256) for security.
 */
public class User {
    
    // ============================================================================== //
    //                                   PROPERTIES                                   //
    // ============================================================================== //
    
    private int id;
    private String username;
    private String passwordHash; // SHA-256 hash of password
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    
    
    // ============================================================================== //
    //                                  CONSTRUCTORS                                  //
    // ============================================================================== //
    
    /**
     * Default constructor.
     */
    public User() {
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * Constructor with username and password hash.
     * 
     * @param username User's login name
     * @param passwordHash Hashed password (SHA-256)
     */
    public User(
        String username, 
        String passwordHash
    ) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now();
    }
    
    
    // ============================================================================== //
    //                              GETTERS AND SETTERS                               //
    // ============================================================================== //
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
    
    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
    
    
    // ============================================================================== //
    //                                 UTILITY METHODS                                //
    // ============================================================================== //
    
    @Override
    public String toString() {
        return String.format(
            "User{id=%d, username='%s', createdAt=%s, lastLoginAt=%s}",
            id, 
            username, 
            createdAt, 
            lastLoginAt
        );
    }
}
