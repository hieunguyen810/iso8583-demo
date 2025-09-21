package com.example.demo.server;

import com.example.demo.model.Iso8583Message;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Order(1)
public class Iso8583Server {
    private static final int PORT = 8583;
    private final ExecutorService clientHandlerPool = Executors.newFixedThreadPool(10);
    private final ScheduledExecutorService scheduledTaskExecutor = Executors.newSingleThreadScheduledExecutor();

    @EventListener(ApplicationReadyEvent.class)
    public void startServer() {
        String mode = System.getProperty("app.mode", "both");
        if (!"client".equals(mode)) {
            new Thread(this::runServer).start();
            System.out.println("🚀 ISO 8583 Server starting on port " + PORT);
        }
    }

    private void runServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ Server ready and listening...");
            System.out.println("📋 Supported message types: 0200 (Auth), 0800 (Echo)");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientHandlerPool.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("❌ Server error: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        String clientAddress = clientSocket.getRemoteSocketAddress().toString();
        System.out.println("🔌 Client connected: " + clientAddress);

        Future<?> periodicTask = null;

        try (DataInputStream input = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream())) {

            // Schedule a periodic task to send a message every 10 seconds
            periodicTask = scheduledTaskExecutor.scheduleAtFixedRate(() -> {
                // Synchronize on the output stream to prevent concurrent writes
                synchronized (output) {
                    try {
                        Iso8583Message transaction = getTransaction();
                        String transactionMessage = transaction.toString();
                        System.out.println("📤 [" + clientAddress + "] Periodically sending: " + transactionMessage);
                        output.writeShort(transactionMessage.length());
                        output.writeBytes(transactionMessage);
                        output.flush();
                    } catch (IOException e) {
                        System.err.println("❌ [" + clientAddress + "] Periodic send error: " + e.getMessage());
                        // If an error occurs, the task will stop itself or the main loop will handle the socket closure.
                    }
                }
            }, 10, 10, TimeUnit.SECONDS);

            // Handle incoming messages on the same connection
            while (!clientSocket.isClosed() && clientSocket.isConnected()) {
                try {
                    int messageLength = input.readUnsignedShort();
                    byte[] messageBytes = new byte[messageLength];
                    input.readFully(messageBytes);
                    String isoMessage = new String(messageBytes);
                    System.out.println("📨 [" + clientAddress + "] Received: " + isoMessage);

                    Iso8583Message request = parseMessage(isoMessage);
                    Iso8583Message response = processMessage(request);
                    String responseMessage = response.toString();
                    
                    // Synchronize on the output stream before writing
                    synchronized (output) {
                        output.writeShort(responseMessage.length());
                        output.writeBytes(responseMessage);
                        output.flush();
                    }
                    
                    System.out.println("📤 [" + clientAddress + "] Sent: " + responseMessage);
                } catch (EOFException e) {
                    System.out.println("👋 [" + clientAddress + "] Client disconnected");
                    break;
                } catch (IOException e) {
                    System.err.println("🔌 [" + clientAddress + "] Connection error: " + e.getMessage());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ [" + clientAddress + "] Error handling client: " + e.getMessage());
        } finally {
            if (periodicTask != null) {
                periodicTask.cancel(true); // Stop the periodic task
            }
            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
                System.out.println("🔌 [" + clientAddress + "] Connection closed");
            } catch (IOException e) {
                System.err.println("❌ Error closing client socket: " + e.getMessage());
            }
        }
    }

    // --- All other methods (parseMessage, getTransaction, processMessage, etc.) remain the same ---
    private Iso8583Message parseMessage(String message) {
        Iso8583Message msg = new Iso8583Message();
        if (message.length() >= 4) {
            String mti = message.substring(0, 4);
            msg.setMti(mti);
            String[] parts = message.split("\\|");
            for (int i = 1; i < parts.length; i++) {
                String[] fieldValue = parts[i].split("=", 2);
                if (fieldValue.length == 2) {
                    try {
                        int fieldNumber = Integer.parseInt(fieldValue[0]);
                        msg.addField(fieldNumber, fieldValue[1]);
                    } catch (NumberFormatException e) {
                        System.err.println("⚠️ Invalid field number: " + fieldValue[0]);
                    }
                }
            }
        }
        return msg;
    }

    private Iso8583Message getTransaction() {
        Iso8583Message transaction = new Iso8583Message();
        transaction.setMti("0200");
        transaction.addField(2, "0000000000000000");
        return transaction;
    }

    private Iso8583Message processMessage(Iso8583Message request) {
        Iso8583Message response = new Iso8583Message();
        String requestMti = request.getMti();

        if ("0200".equals(requestMti)) {
            response.setMti("0210");
            copyField(request, response, 2);
            copyField(request, response, 3);
            copyField(request, response, 4);
            copyField(request, response, 7);
            copyField(request, response, 11);
            copyField(request, response, 37);
            response.addField(38, generateApprovalCode());
            response.addField(39, "00");
            System.out.println("💳 Processed authorization request - APPROVED");
        } else if ("0800".equals(requestMti)) {
            response.setMti("0810");
            copyField(request, response, 7);
            copyField(request, response, 11);
            copyField(request, response, 70);
            response.addField(7, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss")));
            System.out.println("💓 Processed echo request - Connection alive");
        } else {
            response.setMti("0210");
            response.addField(39, "30");
            System.err.println("⚠️ Unknown message type: " + requestMti);
        }
        return response;
    }

    private void copyField(Iso8583Message source, Iso8583Message target, int fieldNumber) {
        String value = source.getField(fieldNumber);
        if (value != null) {
            target.addField(fieldNumber, value);
        }
    }

    private String generateApprovalCode() {
        return String.format("%06d", (int) (Math.random() * 999999));
    }
}