package database;

import util.Logger;
import java.sql.*;
import java.io.File;

/**
 * DatabaseManager - Handles database initialization and configuration for Manager
 * Use SessionManager, ProcessManager, and ComputerManager for data operations
 * Improved with structured logging and better error handling
 * Refactored to use dependency injection
 */
public class DatabaseManager {

    private static final String COMPONENT = "DatabaseManager";
    private final String dbUrl;
    
    /**
     * Constructor with dependency injection
     * @param dbUrl Database URL to use
     */
    public DatabaseManager(String dbUrl) {
        this.dbUrl = dbUrl;
        ensureDirectoryExists(dbUrl);
    }
    
    /**
     * Ensure the directory for the database file exists
     * @param dbUrl Database URL
     */
    private void ensureDirectoryExists(String dbUrl) {
        try {
            // Extract file path from jdbc:sqlite:path/to/file.db
            String filePath = dbUrl.replace("jdbc:sqlite:", "");
            File dbFile = new File(filePath);
            File parentDir = dbFile.getParentFile();
            
            if (parentDir != null && !parentDir.exists()) {
                if (parentDir.mkdirs()) {
                    Logger.info(COMPONENT, "Created database directory: " + parentDir.getAbsolutePath());
                } else {
                    Logger.error(COMPONENT, "Failed to create database directory: " + parentDir.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            Logger.error(COMPONENT, "Error ensuring database directory exists", e);
        }
    }
    
    /**
     * Initialize database tables
     */
    public void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            
            // Create Computer table
            String sqlComputer = "CREATE TABLE IF NOT EXISTS computer ("
                    + " mac_address TEXT PRIMARY KEY,"
                    + " hostname TEXT UNIQUE NOT NULL,"
                    + " ip_address TEXT NOT NULL,"
                    + " os TEXT,"
                    + " architecture TEXT,"
                    + " manufacturer TEXT,"
                    + " model TEXT,"
                    + " serial_number TEXT,"
                    + " cpu_name TEXT,"
                    + " cpu_vendor TEXT,"
                    + " physical_cores INTEGER,"
                    + " logical_cores INTEGER,"
                    + " cpu_max_freq INTEGER"
                    + ");";
            stmt.execute(sqlComputer);
            
            // Create Session table
            String sqlSession = "CREATE TABLE IF NOT EXISTS session ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " mac_address TEXT,"     // Reference to computer mac_address
                    + " cpu_usage REAL,"
                    + " total_ram INTEGER,"
                    + " ram_usage INTEGER,"
                    + " timestamp INTEGER"
                    + ");";
            stmt.execute(sqlSession);

            // Create Process table
            String sqlProcess = "CREATE TABLE IF NOT EXISTS process ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " pid INTEGER,"
                    + " session_id INTEGER,"
                    + " name TEXT,"
                    + " cpu_usage REAL,"
                    + " ram_usage INTEGER,"
                    + " FOREIGN KEY (session_id) REFERENCES session (id),"
                    + " UNIQUE (pid, session_id)"  // Prevent duplicate processes per session
                    + ");";
            stmt.execute(sqlProcess);
            
            // Create Users table for authentication
            String sqlUsers = "CREATE TABLE IF NOT EXISTS users ("
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " username TEXT UNIQUE NOT NULL,"
                    + " password_hash TEXT NOT NULL,"
                    + " created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + " last_login_at TIMESTAMP"
                    + ");";
            stmt.execute(sqlUsers);
            
            // Removed initialization log - not critical for verbose mode
            
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error initializing database", e);
        }
    }
    
    /**
     * Get database connection
     * @return Connection to the database
     * @throws SQLException if connection cannot be established
     */
    public Connection getConnection() throws SQLException {
        if (dbUrl == null || dbUrl.isEmpty()) {
            throw new SQLException("Database URL is not initialized.");
        }
        return DriverManager.getConnection(dbUrl);
    }
    
    /**
     * Connect to database (for AuthRepository compatibility).
     * 
     * @return Connection to the database, or null if failed
     */
    public Connection connect() {
        try {
            return getConnection();
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Failed to connect to database", e);
            return null;
        }
    }
    
    /**
     * Disconnect from database.
     * 
     * @param conn Connection to close
     */
    public void disconnect(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                Logger.error(COMPONENT, "Error closing database connection", e);
            }
        }
    }
}