package database;

import model.User;
import util.Logger;

import java.sql.*;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

/**
 * Repository for User authentication operations.
 * 
 * Handles:
 * - User authentication (login)
 * - Password hashing (SHA-256)
 * - User creation (for initial admin setup)
 * - Last login tracking
 */
public class AuthRepository {
    
    private static final String COMPONENT = "AuthRepository";
    
    // ============================================================================== //
    //                                   DEPENDENCIES                                 //
    // ============================================================================== //
    
    private final DatabaseManager databaseManager;
    
    
    // ============================================================================== //
    //                                  CONSTRUCTOR                                   //
    // ============================================================================== //
    
    /**
     * Constructor with Dependency Injection.
     * 
     * @param databaseManager Database connection manager
     */
    public AuthRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
    
    
    // ============================================================================== //
    //                              AUTHENTICATION METHODS                            //
    // ============================================================================== //
    
    /**
     * Authenticate user with username and password.
     * 
     * Workflow:
     * ### Step 1: Hash the provided password
     * ### Step 2: Query database for user with matching username and password hash
     * ### Step 3: If found, update last login timestamp
     * ### Step 4: Return user object or null
     * 
     * @param username User's login name
     * @param password User's plain-text password
     * @return User object if authenticated, null otherwise
     */
    public User authenticate(
        String username, 
        String password
    ) {
        Connection conn = databaseManager.connect();
        if (conn == null) {
            Logger.error(COMPONENT, "Failed to connect to database for authentication");
            return null;
        }
        
        try {
            // ### 1. Hash the password
            String passwordHash = hashPassword(password);
            
            // ### 2. Query for user
            String sql = "SELECT id, username, password_hash, created_at, last_login_at " +
                        "FROM users WHERE username = ? AND password_hash = ?";
            
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                // ### 3. User found - create User object
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPasswordHash(rs.getString("password_hash"));
                
                Timestamp createdAtTs = rs.getTimestamp("created_at");
                if (createdAtTs != null) {
                    user.setCreatedAt(createdAtTs.toLocalDateTime());
                }
                
                Timestamp lastLoginTs = rs.getTimestamp("last_login_at");
                if (lastLoginTs != null) {
                    user.setLastLoginAt(lastLoginTs.toLocalDateTime());
                }
                
                // ### 4. Update last login timestamp (in same connection)
                String updateSql = "UPDATE users SET last_login_at = ? WHERE id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                updateStmt.setInt(2, user.getId());
                updateStmt.executeUpdate();
                updateStmt.close();
                
                user.setLastLoginAt(LocalDateTime.now());
                
                Logger.info(COMPONENT, "User '" + username + "' authenticated successfully");
                return user;
            } else {
                Logger.warn(COMPONENT, "Authentication failed for user '" + username + "'");
                return null;
            }
            
        } catch (SQLException e) {
            Logger.error(COMPONENT, "SQL error during authentication", 
                        new Exception(e.getMessage()));
            return null;
        } finally {
            databaseManager.disconnect(conn);
        }
    }
    
    
    // ============================================================================== //
    //                                USER MANAGEMENT                                 //
    // ============================================================================== //
    
    /**
     * Create a new user account.
     * 
     * ### Step 1: Hash the password
     * ### Step 2: Insert user into database
     * ### Step 3: Return created user with ID
     * 
     * @param username User's login name
     * @param password User's plain-text password
     * @return Created User object, or null if failed
     */
    public User createUser(
        String username, 
        String password
    ) {
        Connection conn = databaseManager.connect();
        if (conn == null) {
            Logger.error(COMPONENT, "Failed to connect to database for user creation");
            return null;
        }
        
        try {
            // ### 1. Hash password
            String passwordHash = hashPassword(password);
            
            // ### 2. Insert user
            String sql = "INSERT INTO users (username, password_hash, created_at) " +
                        "VALUES (?, ?, ?)";
            
            PreparedStatement pstmt = conn.prepareStatement(sql, 
                                        Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                // ### 3. Get generated ID
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    User user = new User(username, passwordHash);
                    user.setId(generatedKeys.getInt(1));
                    user.setCreatedAt(LocalDateTime.now());
                    
                    Logger.success(COMPONENT, "User '" + username + "' created successfully");
                    return user;
                }
            }
            
            Logger.error(COMPONENT, "Failed to create user '" + username + "'");
            return null;
            
        } catch (SQLException e) {
            Logger.error(COMPONENT, "SQL error during user creation", 
                        new Exception(e.getMessage()));
            return null;
        } finally {
            databaseManager.disconnect(conn);
        }
    }
    
    /**
     * Check if a user exists.
     * 
     * @param username Username to check
     * @return true if user exists, false otherwise
     */
    public boolean userExists(String username) {
        Connection conn = databaseManager.connect();
        if (conn == null) {
            return false;
        }
        
        try {
            String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
            
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error checking user existence", 
                        new Exception(e.getMessage()));
            return false;
        } finally {
            databaseManager.disconnect(conn);
        }
    }
    
    /**
     * Initialize default admin user if no users exist.
     * 
     * Creates admin/admin account for first-time setup.
     */
    public void initializeDefaultAdmin() {
        if (!userExists("admin")) {
            User admin = createUser("admin", "admin");
            if (admin != null) {
                Logger.success(COMPONENT, "Default admin account created (username: admin, password: admin)");
            }
        }
    }
    
    
    // ============================================================================== //
    //                                 HELPER METHODS                                 //
    // ============================================================================== //
    
    /**
     * Hash password using SHA-256.
     * 
     * @param password Plain-text password
     * @return Hexadecimal hash string
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            
            // Convert byte array to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            Logger.error(COMPONENT, "SHA-256 algorithm not available", 
                        new Exception(e.getMessage()));
            // Fallback to plain text (NOT SECURE - only for compatibility)
            return password;
        }
    }
}
