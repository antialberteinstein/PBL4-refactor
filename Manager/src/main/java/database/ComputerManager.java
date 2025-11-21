package database;

import model.Computer;
import util.Logger;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Manager class for Computer operations in Manager module
 * Improved with better logging and error handling
 * Refactored to use dependency injection
 */
public class ComputerManager {
    
    private static final String COMPONENT = "ComputerManager";
    private final DatabaseManager databaseManager;
    
    /**
     * Constructor with dependency injection
     * @param databaseManager The database manager instance
     */
    public ComputerManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
    
    /**
     * Update or insert computer information (upsert based on MAC address)
     * @param computer Computer object with all information
     * @return true if successful
     */
    public boolean saveComputer(Computer computer) {
        try (Connection conn = databaseManager.getConnection()) {
            // First check if computer already exists
            boolean exists = computerExists(computer.getMacAddress());
            
            String sql = "INSERT OR REPLACE INTO computer " +
                        "(mac_address, hostname, ip_address, os, architecture, manufacturer, model, serial_number, " +
                        "cpu_name, cpu_vendor, physical_cores, logical_cores, cpu_max_freq) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, computer.getMacAddress());  // Primary key first
                pstmt.setString(2, computer.getHostname());
                pstmt.setString(3, computer.getIpAddress());
                pstmt.setString(4, computer.getOs());
                pstmt.setString(5, computer.getArchitecture());
                pstmt.setString(6, computer.getManufacturer());
                pstmt.setString(7, computer.getModel());
                pstmt.setString(8, computer.getSerialNumber());
                pstmt.setString(9, computer.getCpuName());
                pstmt.setString(10, computer.getCpuVendor());
                pstmt.setInt(11, computer.getPhysicalCores());
                pstmt.setInt(12, computer.getLogicalCores());
                pstmt.setLong(13, computer.getCpuMaxFreq());

                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    if (exists) {
                        Logger.info(COMPONENT, "Computer information updated: " + computer.getHostname() + " (" + computer.getMacAddress() + ")");
                    } else {
                        Logger.success(COMPONENT, "Computer information registered for the first time: " + computer.getHostname() + " (" + computer.getMacAddress() + ")");
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error saving computer information", e);
        }
        return false;
    }
    
    /**
     * Check if computer already exists in database
     * @param macAddress MAC address to check
     * @return true if computer exists
     */
    private boolean computerExists(String macAddress) {
        try (Connection conn = databaseManager.getConnection()) {
            String sql = "SELECT 1 FROM computer WHERE mac_address = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, macAddress);
                try (ResultSet rs = pstmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error checking if computer exists", e);
        }
        return false;
    }
    
    /**
     * Get IP address by MAC address
     * @param macAddress MAC address to look up
     * @return IP address or null if not found
     */
    public String getIpByMac(String macAddress) {
        try (Connection conn = databaseManager.getConnection()) {
            String sql = "SELECT ip_address FROM computer WHERE mac_address = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, macAddress);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("ip_address");
                    }
                }
            }
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error getting IP by MAC address", e);
        }
        return null;
    }
    
    /**
     * Get the current computer information
     * @return Computer object or null if not found
     */
    public Computer getComputerByMacAddress(String macAddress) {
        try (Connection conn = databaseManager.getConnection()) {
            String sql = "SELECT * FROM computer WHERE mac_address = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, macAddress);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Computer computer = new Computer();
                        computer.setMacAddress(rs.getString("mac_address"));
                        computer.setHostname(rs.getString("hostname"));
                        computer.setIpAddress(rs.getString("ip_address"));
                        computer.setOs(rs.getString("os"));
                        computer.setArchitecture(rs.getString("architecture"));
                        computer.setManufacturer(rs.getString("manufacturer"));
                        computer.setModel(rs.getString("model"));
                        computer.setSerialNumber(rs.getString("serial_number"));
                        computer.setCpuName(rs.getString("cpu_name"));
                        computer.setCpuVendor(rs.getString("cpu_vendor"));
                        computer.setPhysicalCores(rs.getInt("physical_cores"));
                        computer.setLogicalCores(rs.getInt("logical_cores"));
                        computer.setCpuMaxFreq(rs.getLong("cpu_max_freq"));
                        return computer;
                    }
                }
            }
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error retrieving computer information", e);
        }
        return null;
    }

    public List<Computer> getAllComputers() {
        List<Computer> computers = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection()) {
            String sql = "SELECT * FROM computer";
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Computer computer = new Computer();
                    computer.setMacAddress(rs.getString("mac_address"));
                    computer.setHostname(rs.getString("hostname"));
                    computer.setIpAddress(rs.getString("ip_address"));
                    computer.setOs(rs.getString("os"));
                    computer.setArchitecture(rs.getString("architecture"));
                    computer.setManufacturer(rs.getString("manufacturer"));
                    computer.setModel(rs.getString("model"));
                    computer.setSerialNumber(rs.getString("serial_number"));
                    computer.setCpuName(rs.getString("cpu_name"));
                    computer.setCpuVendor(rs.getString("cpu_vendor"));
                    computer.setPhysicalCores(rs.getInt("physical_cores"));
                    computer.setLogicalCores(rs.getInt("logical_cores"));
                    computer.setCpuMaxFreq(rs.getLong("cpu_max_freq"));
                    computers.add(computer);
                }
            }
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error retrieving all computers", e);
        }
        return computers;
    }


    public Map<String, String> getAllComputerIpMap() {
        Map<String, String> ipMap = new HashMap<>();
        try (Connection conn = databaseManager.getConnection()) {
            String sql = "SELECT mac_address, ip_address FROM computer";
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String mac = rs.getString("mac_address");
                    String ip = rs.getString("ip_address");
                    ipMap.put(mac, ip);
                }
            }
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error retrieving computer IP map", e);
        }
        return ipMap;
    }

    public int getComputerCount() {
        try (Connection conn = databaseManager.getConnection()) {
            String sql = "SELECT COUNT(*) FROM computer";
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error counting computers", e);
        }
        return 0;
    }

    /**
     * Get computer by MAC address
     * @param macAddress MAC address to search for
     * @return Computer object or null if not found
     */
    public Computer getComputerByMac(String macAddress) {
        try (Connection conn = databaseManager.getConnection()) {
            String sql = "SELECT * FROM computer WHERE mac_address = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, macAddress);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Computer computer = new Computer();
                        computer.setMacAddress(rs.getString("mac_address"));
                        computer.setHostname(rs.getString("hostname"));
                        computer.setIpAddress(rs.getString("ip_address"));
                        computer.setOs(rs.getString("os"));
                        computer.setArchitecture(rs.getString("architecture"));
                        computer.setManufacturer(rs.getString("manufacturer"));
                        computer.setModel(rs.getString("model"));
                        computer.setSerialNumber(rs.getString("serial_number"));
                        computer.setCpuName(rs.getString("cpu_name"));
                        computer.setCpuVendor(rs.getString("cpu_vendor"));
                        computer.setPhysicalCores(rs.getInt("physical_cores"));
                        computer.setLogicalCores(rs.getInt("logical_cores"));
                        computer.setCpuMaxFreq(rs.getLong("cpu_max_freq"));
                        return computer;
                    }
                }
            }
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error retrieving computer by MAC: " + macAddress, e);
        }
        return null;
    }
}
