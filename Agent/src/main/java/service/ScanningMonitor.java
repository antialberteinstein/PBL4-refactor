package service;

import util.ProtocolManager;
import model.Computer;

/**
 * Handles scanning requests from Manager
 * Follows Single Responsibility Principle - only handles scanning protocol
 * Refactored to use ComputerRetriever directly
 */
public class ScanningMonitor {

    private final ProtocolManager protocolManager;
    private final ComputerRetriever computerRetriever;

    public ScanningMonitor(ProtocolManager protocolManager, ComputerRetriever computerRetriever) {
        this.protocolManager = protocolManager;
        this.computerRetriever = computerRetriever;
    }

    // Kiểm tra xem có tin nhắn yêu cầu scan không
    // Nếu có, thực hiện scan và gửi kết quả về Manager
    public String checkMessage(String message) {
        String prefix = protocolManager.HELLO_REQUEST;

        if (message.startsWith(prefix)) {
            Computer computer = computerRetriever.getCurrentComputer();
            String response = protocolManager.HELLO_RESPONSE + protocolManager.SEPARATOR + computer.getIpAddress();
            return response;
        }
        return null;
    }
}
