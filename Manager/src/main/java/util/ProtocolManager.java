package util;

public class ProtocolManager {
    // Base protocol constants
    public final String PROTOCOL_PREFIX = "<NHOM3>";
    public final String SEPARATOR = "|";
    
    // Discovery protocol
    public final String HELLO_REQUEST = PROTOCOL_PREFIX + "HELLO";
    public final String HELLO_RESPONSE = PROTOCOL_PREFIX + "HELLO_CON_CAC";

    // Computer information protocol
    public final String GET_COMPUTER_INFO_REQUEST = PROTOCOL_PREFIX + "GET_COMPUTER_INFO";
    public final String GET_COMPUTER_INFO_RESPONSE = PROTOCOL_PREFIX + "COMPUTER_INFO";

    // Session and process protocol
    public final String GET_SESSION_REQUEST = PROTOCOL_PREFIX + "GET_SESSION";
    public final String GET_SESSION_RESPONSE = PROTOCOL_PREFIX + "SESSION";
    public final String PROCESS_RESPONSE = PROTOCOL_PREFIX + "PROCESS";
    public final String TIMESTAMP_RESPONSE = PROTOCOL_PREFIX + "TIMESTAMP";
}
