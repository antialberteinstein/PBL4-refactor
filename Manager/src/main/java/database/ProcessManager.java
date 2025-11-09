package database;

import model.Process;
import util.Logger;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Manager class for Process operations in Manager module
 * Improved with structured logging and better error handling
 * Refactored to use dependency injection
 */
public class ProcessManager {

    private static final String COMPONENT = "ProcessManager";
    private final DatabaseManager databaseManager;

    /**
     * Constructor with dependency injection
     * @param databaseManager The database manager instance
     */
    public ProcessManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
    
    public boolean saveProcess(Process process) {
        try (Connection conn = databaseManager.getConnection()) {
            // Use INSERT OR IGNORE to prevent duplicate (pid, session_id) combinations
            String sql = "INSERT OR IGNORE INTO process (pid, session_id, name, cpu_usage, ram_usage) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, process.getPid());
                pstmt.setLong(2, process.getSessionId());
                pstmt.setString(3, process.getName());
                pstmt.setDouble(4, process.getCpuUsage());
                pstmt.setLong(5, process.getRamUsage());

                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    // Removed individual process log - too verbose, use batch summary instead
                    return true;
                }
            }
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error saving process", e);
        }
        return false;
    }

    public boolean saveProcesses(int sessionId, List<Process> processes, Connection conn) {
        try {
            // Use INSERT OR IGNORE to prevent duplicate (pid, session_id) combinations
            String sql = "INSERT OR IGNORE INTO process (pid, session_id, name, cpu_usage, ram_usage) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Process process : processes) {
                    pstmt.setLong(1, process.getPid());
                    pstmt.setInt(2, sessionId);
                    pstmt.setString(3, process.getName());
                    pstmt.setDouble(4, process.getCpuUsage());
                    pstmt.setLong(5, process.getRamUsage());
                    pstmt.addBatch();
                }
                int[] rowsAffected = pstmt.executeBatch();
                
                int totalInserted = 0;
                for (int count : rowsAffected) {
                    totalInserted += count;
                }
                
                if (totalInserted > 0) {
                    Logger.success(COMPONENT, totalInserted + " processes saved for session ID: " + sessionId);
                    return true;
                }
            }
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error saving processes", e);
        }
        return false;
    }

    public boolean saveProcesses(int sessionId, List<Process> processes) {
        try (Connection conn = databaseManager.getConnection()) {
            return saveProcesses(sessionId, processes, conn);
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error obtaining database connection", e);
        }
        return false;
    }

    /**
     * Get all processes for a specific session
     * @param sessionId The session ID
     * @return List of Process
     */
    public List<Process> getProcessesBySessionId(int sessionId) {
        List<Process> processes = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection()) {
            String processSql = "SELECT * FROM process WHERE session_id = ? ORDER BY cpu_usage DESC";
            try (PreparedStatement processPstmt = conn.prepareStatement(processSql)) {
                processPstmt.setInt(1, sessionId);
                try (ResultSet processRs = processPstmt.executeQuery()) {
                    while (processRs.next()) {
                        int pid = processRs.getInt("pid");
                        String name = processRs.getString("name");
                        double cpuUsage = processRs.getDouble("cpu_usage");
                        long ramUsage = processRs.getLong("ram_usage");
                        
                        Process process = new Process(pid, sessionId, name, cpuUsage, ramUsage);
                        process.setId(processRs.getInt("id"));
                        processes.add(process);
                    }
                }
            }
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error retrieving processes for session " + sessionId, e);
        }
        return processes;
    }
    
    /**
     * Get count of processes for a specific session
     * @param sessionId The session ID to count processes for
     * @return Number of processes in the session
     */
    public int getProcessCount(int sessionId) {
        try (Connection conn = databaseManager.getConnection()) {
            String sql = "SELECT COUNT(*) FROM process WHERE session_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, sessionId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error counting processes for session " + sessionId + ": " + e.getMessage());
        }
        return 0;
    }
}
