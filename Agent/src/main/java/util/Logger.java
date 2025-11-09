package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Structured logging utility that replaces System.out.println calls
 * Follows Single Responsibility Principle - only handles logging
 * Provides consistent, structured log format across the application
 */
public class Logger {
    
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Log levels
    public enum Level {
        DEBUG("DEBUG", "\u001B[36m"),   // Cyan
        INFO("INFO", "\u001B[32m"),     // Green
        WARN("WARN", "\u001B[33m"),     // Yellow
        ERROR("ERROR", "\u001B[31m"),   // Red
        SUCCESS("SUCCESS", "\u001B[32m"); // Green
        
        private final String name;
        private final String color;
        
        Level(String name, String color) {
            this.name = name;
            this.color = color;
        }
        
        public String getName() { return name; }
        public String getColor() { return color; }
    }
    
    private static final String RESET = "\u001B[0m";
    private static boolean enableColors = true;
    
    /**
     * Log a message with specified level
     */
    public static void log(Level level, String component, String message) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String colorCode = enableColors ? level.getColor() : "";
        String resetCode = enableColors ? RESET : "";
        
        System.out.printf("%s[%s] %s%-5s%s [%s] %s%n",
            colorCode,
            timestamp,
            colorCode,
            level.getName(),
            resetCode,
            component,
            message
        );
    }
    
    /**
     * Log debug message
     */
    public static void debug(String component, String message) {
        log(Level.DEBUG, component, message);
    }
    
    /**
     * Log info message
     */
    public static void info(String component, String message) {
        log(Level.INFO, component, message);
    }
    
    /**
     * Log warning message
     */
    public static void warn(String component, String message) {
        log(Level.WARN, component, message);
    }
    
    /**
     * Log error message
     */
    public static void error(String component, String message) {
        log(Level.ERROR, component, message);
    }
    
    /**
     * Log error message with exception
     */
    public static void error(String component, String message, Exception e) {
        log(Level.ERROR, component, message + " - " + e.getMessage());
        if (enableStackTrace()) {
            e.printStackTrace();
        }
    }
    
    /**
     * Log success message
     */
    public static void success(String component, String message) {
        log(Level.SUCCESS, component, message);
    }
    
    /**
     * Disable color output (useful for file logging)
     */
    public static void disableColors() {
        enableColors = false;
    }
    
    /**
     * Enable color output
     */
    public static void enableColors() {
        enableColors = true;
    }
    
    /**
     * Check if stack trace should be enabled (can be configured)
     */
    private static boolean enableStackTrace() {
        return true; // Can be made configurable
    }
}