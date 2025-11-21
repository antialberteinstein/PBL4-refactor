package database;

import model.Session;
import util.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manager class for Session operations in Manager module
 * Improved with structured logging and better error handling
 * Refactored to use dependency injection
 */
public class SessionManager {

    private static final String COMPONENT = "SessionManager";
    private final DatabaseManager databaseManager;
    private final ProcessManager processManager;
    
    /**
     * Constructor with dependency injection
     * @param databaseManager The database manager instance
     * @param processManager The process manager instance
     */
    public SessionManager(DatabaseManager databaseManager, ProcessManager processManager) {
        this.databaseManager = databaseManager;
        this.processManager = processManager;
    }
    
    /**
     * Save session data
     * @param session The session data to save
     * @return The generated session ID
     */
    public int saveSession(Session session) {
        try (Connection conn = databaseManager.getConnection()) {
            // Start transaction
            conn.setAutoCommit(false);
            
            
            // Insert session data
            String sessionSql = "INSERT INTO session (mac_address, cpu_usage, total_ram, ram_usage, timestamp) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sessionSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, session.getMacAddress());
                pstmt.setDouble(2, session.getCpuUsage());
                pstmt.setLong(3, session.getTotalRam());
                pstmt.setLong(4, session.getRamUsage());
                pstmt.setLong(5, session.getTimestamp());
                
                pstmt.executeUpdate();
                
                // Get the generated session ID
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int sessionId = generatedKeys.getInt(1);
                        
                        // Save associated processes
                        if (session.getProcesses() != null && !session.getProcesses().isEmpty()) {
                            processManager.saveProcesses(sessionId, session.getProcesses(), conn);
                        }
                        
                        // Commit transaction
                        conn.commit();
                        return sessionId;
                    }
                }
            }
            
            // If we reach here, something went wrong
            conn.rollback();
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error saving system data", e);
        }
        return -1;
    }

    public long getSessionIdByMacAndTimestamp(String macAddress, long timestamp) {
        try (Connection conn = databaseManager.getConnection()) {
            String sql = "SELECT id FROM session WHERE mac_address = ? AND timestamp = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, macAddress);
                pstmt.setLong(2, timestamp);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("id");
                    }
                }
            }
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error retrieving session ID for MAC: " + macAddress + " and timestamp: " + timestamp, e);
        }
        return -1;
    }

    /**
     * Get all sessions for a specific computer (MAC address)
     * @param macAddress The MAC address
     * @param limit Max number of sessions to return (-1 for all)
     * @return List of sessions ordered by timestamp ASC (oldest first for charting)
     */
    public List<Session> getSessionsByMac(String macAddress, int limit) {
        List<Session> sessions = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection()) {
            String sql;
            if (limit < 0) {
                // Get all sessions, ordered oldest first for time-series display
                sql = "SELECT * FROM session WHERE mac_address = ? ORDER BY timestamp ASC";
            } else {
                // Get latest N sessions, ordered oldest first
                sql = "SELECT * FROM (SELECT * FROM session WHERE mac_address = ? ORDER BY timestamp DESC LIMIT ?) ORDER BY timestamp ASC";
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, macAddress);
                if (limit >= 0) {
                    pstmt.setInt(2, limit);
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        sessions.add(mapResultSetToSession(rs));
                    }
                }
            }
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error retrieving sessions by MAC: " + macAddress, e);
        }
        return sessions;
    }

    /**
     * Get session by ID
     * @param sessionId Session ID to retrieve
     * @return Session object or null if not found
     */
    public Session getSessionById(int sessionId) {
        try (Connection conn = databaseManager.getConnection()) {
            String sql = "SELECT * FROM session WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, sessionId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToSession(rs);
                    }
                }
            }
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error retrieving session by ID: " + sessionId, e);
        }
        return null;
    }
    
    /**
     * Get latest session for a specific MAC address
     * @param macAddress MAC address of the computer
     * @return Latest Session object or null if not found
     */
    public Session getLatestSessionByMac(String macAddress) {
        try (Connection conn = databaseManager.getConnection()) {
            String sql = "SELECT * FROM session WHERE mac_address = ? ORDER BY timestamp DESC LIMIT 1";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, macAddress);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToSession(rs);
                    }
                }
            }
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error retrieving latest session for MAC: " + macAddress, e);
        }
        return null;
    }
    
    /**
     * Map ResultSet to Session object
     */
    private Session mapResultSetToSession(ResultSet rs) throws SQLException {
        Session session = new Session();
        session.setId(rs.getLong("id"));
        session.setMacAddress(rs.getString("mac_address"));
        session.setCpuUsage(rs.getDouble("cpu_usage"));
        session.setTotalRam(rs.getLong("total_ram"));
        session.setRamUsage(rs.getLong("ram_usage"));
        session.setTimestamp(rs.getLong("timestamp"));
        return session;
    }
}

