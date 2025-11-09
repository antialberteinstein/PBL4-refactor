package model.entity;

import java.time.LocalDateTime;

/**
 * User entity for web authentication.
 * 
 * Represents a user account with credentials for accessing the ManagerWeb system.
 */
public class User {
    
    // ============================================================================== //
    //                                   PROPERTIES                                   //
    // ============================================================================== //
    
    private int id;
    private String username;
    private String passwordHash;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    
    
    // ============================================================================== //
    //                                  CONSTRUCTORS                                  //
    // ============================================================================== //
    
    public User() {
        this.createdAt = LocalDateTime.now();
    }
    
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
}
