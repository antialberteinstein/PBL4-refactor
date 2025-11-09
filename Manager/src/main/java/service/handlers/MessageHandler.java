package service.handlers;

import java.util.List;

/**
 * Interface for message handlers that process incoming messages and generate responses
 * Follows Single Responsibility Principle and Interface Segregation Principle
 */
public interface MessageHandler {
    
    /**
     * Process an incoming message and return responses
     * @param message The incoming message to process
     * @return List of response messages, empty list if no response needed, null if message not handled
     */
    List<String> handleMessage(String message);
    
    /**
     * Check if this handler can process the given message
     * @param message The message to check
     * @return true if this handler can process the message, false otherwise
     */
    boolean canHandle(String message);
}