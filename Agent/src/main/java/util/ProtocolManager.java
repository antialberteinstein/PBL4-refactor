package util;

/**
 * ProtocolManager - Centralized protocol constants for Agent communication
 *
 * Note: constants are static so callers can reference them without creating an instance.
 */
public class ProtocolManager {
    // Base protocol constants
    public static final String PROTOCOL_PREFIX = "<NHOM3>";
    public static final String SEPARATOR = "|";

    // Discovery protocol
    public static final String HELLO_REQUEST = PROTOCOL_PREFIX + "HELLO";
    public static final String HELLO_RESPONSE = PROTOCOL_PREFIX + "HELLO_CON_CAC";

    // Computer information protocol
    public static final String GET_COMPUTER_INFO_REQUEST = PROTOCOL_PREFIX + "GET_COMPUTER_INFO";
    public static final String GET_COMPUTER_INFO_RESPONSE = PROTOCOL_PREFIX + "COMPUTER_INFO";

    // Session and process protocol
    public static final String GET_SESSION_REQUEST = PROTOCOL_PREFIX + "GET_SESSION";
    public static final String GET_SESSION_RESPONSE = PROTOCOL_PREFIX + "SESSION";
    public static final String PROCESS_RESPONSE = PROTOCOL_PREFIX + "PROCESS";

    // Remote command protocol (TCP)
    public static final String KILL_PROCESS_REQUEST = PROTOCOL_PREFIX + "KILL_PROCESS";
    public static final String KILL_PROCESS_RESPONSE = PROTOCOL_PREFIX + "KILL_PROCESS_RESULT";
    public static final String SHUTDOWN_REQUEST = PROTOCOL_PREFIX + "SHUTDOWN";
    public static final String SHUTDOWN_RESPONSE = PROTOCOL_PREFIX + "SHUTDOWN_RESULT";
    public static final String SEND_WARNING_REQUEST = PROTOCOL_PREFIX + "SEND_WARNING";
    public static final String SEND_WARNING_RESPONSE = PROTOCOL_PREFIX + "SEND_WARNING_RESULT";
    public static final String SEND_MESSAGE_REQUEST = PROTOCOL_PREFIX + "SEND_MESSAGE";
    public static final String SEND_MESSAGE_RESPONSE = PROTOCOL_PREFIX + "SEND_MESSAGE_RESULT";
}
