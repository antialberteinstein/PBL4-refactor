package service;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

import java.util.ArrayList;
import java.util.List;

import config.AppConfig;

import util.Logger;
// Quản lý việc gửi và nhận tin nhắn giữa Manager và Agent thông qua UDP.
public class NetworkMessageService extends Thread {

    private DatagramSocket mailbox;
    private AppConfig appConfig;
    private List<String> buffer; // Giữ các tin nhắn nhận được.

    private HostScanner hostScanner = null;
    private SessionRetriever sessionRetriever = null;

    public NetworkMessageService(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.buffer = new ArrayList<>();
    }

    public void setHostScanner(HostScanner hostScanner) {
        this.hostScanner = hostScanner;
    }

    public void setSessionRetriever(SessionRetriever sessionRetriever) {
        this.sessionRetriever = sessionRetriever;
    }


    public void open() throws Exception {
        if (mailbox == null || mailbox.isClosed()) {
            mailbox = new DatagramSocket(appConfig.getManagerUdpPort());
        }
    }

    public void close() {
        if (mailbox != null && !mailbox.isClosed()) {
            mailbox.close();
        }
    }

    // Hàm gửi tin nhắn UDP
    // Luôn tạo socket mới để tránh xung đột khi nhiều luồng cùng gửi tin.
    public void sendMessage(String message, String ipAddress, int port) throws Exception {
        if (mailbox == null || mailbox.isClosed()) {
            throw new IllegalStateException("Socket is not open. Call open() before sending messages.");
        }
        
        InetAddress receiverAddress = InetAddress.getByName(ipAddress);
        byte[] buffer = message.getBytes();
        DatagramPacket mail = new DatagramPacket(buffer, buffer.length, receiverAddress, port);
        mailbox.send(mail);
    }

    // Hàm nhận tin nhắn UDP
    public DatagramPacket receiveMessage() throws Exception {
        if (mailbox == null || mailbox.isClosed()) {
            throw new IllegalStateException("Socket is not open. Call open() before receiving messages.");
        }

        byte[] buffer = new byte[1024];
        DatagramPacket mail = new DatagramPacket(buffer, buffer.length);

        mailbox.receive(mail);

        return mail;
    }


    // Chạy luồng riêng để liên tục nhận tin nhắn và lưu vào buffer.
    public void run() {
        while (!isInterrupted()) {
            try {
                DatagramPacket mail = receiveMessage();
                String message = new String(mail.getData(), 0, mail.getLength());

                if (hostScanner != null) {
                    String ip = hostScanner.checkMessage(message);
                    if (ip != null) {
                        // Message processed successfully
                        // Request computer info from Agent (port is now handled by SessionRetriever)
                        sessionRetriever.sendGetComputerInfoRequest(ip);
                        continue;
                    }
                }

                if (sessionRetriever != null) {
                    String result = sessionRetriever.checkMessage(message);
                    if (result != null) {
                        // Tin nhan da duoc xu ly thanh cong.
                        continue;
                    }
                }

                // Neu khong the xu ly tin nhan, them tin nhan vao buffer de xu ly sau.
                buffer.add(message);
            } catch (java.net.SocketException e) {
                // Socket closed, exit gracefully
                if (isInterrupted() || mailbox.isClosed()) {
                    break;
                }
                break;
            } catch (Exception e) {
                // Check if interrupted before continuing
                if (isInterrupted()) {
                    break;
                }
                break;
            }
        }
    }

    // Lấy tin nhắn có liên quan.
    public synchronized List<String> findMailsWithPrefix(String prefix) {

        List<String> relevantMails = new ArrayList<>();
        for (String mail : buffer) {
            if (mail.startsWith(prefix)) {
                relevantMails.add(mail);
            }
        }

        for (String mail : relevantMails) {
            buffer.remove(mail); // Xoá tin nhắn đã lấy khỏi buffer.
        }

        System.out.println("Found " + relevantMails.size() + " relevant mails with prefix: " + prefix);
        return relevantMails;
    }
}
