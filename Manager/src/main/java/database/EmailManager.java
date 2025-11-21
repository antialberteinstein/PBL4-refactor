package database;

import util.Logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class EmailManager {

    private static final String COMPONENT = "EmailManager";
    private final String connectionUrl;

    public EmailManager(String connectionUrl) {
        this.connectionUrl = connectionUrl;
        ensureDirectoryExists();
    }

    private void ensureDirectoryExists() {
        try {
            String filePath = connectionUrl.replace("jdbc:sqlite:", "");
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

    public void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(connectionUrl);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS Email (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "email TEXT UNIQUE NOT NULL)";
            stmt.execute(sql);
            Logger.info(COMPONENT, "Email database initialized.");
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error initializing email database", e);
        }
    }

    public boolean addEmail(String email) {
        String sql = "INSERT INTO Email(email) VALUES(?)";
        try (Connection conn = DriverManager.getConnection(connectionUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.executeUpdate();
            Logger.info(COMPONENT, "Added new email: " + email);
            return true;
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error adding email: " + email, e);
            return false;
        }
    }

    public List<String> getAllEmails() {
        List<String> emails = new ArrayList<>();
        String sql = "SELECT email FROM Email";
        try (Connection conn = DriverManager.getConnection(connectionUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                emails.add(rs.getString("email"));
            }
        } catch (SQLException e) {
            Logger.error(COMPONENT, "Error retrieving emails", e);
        }
        return emails;
    }
}
