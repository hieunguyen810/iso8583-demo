package com.example.demo.client;

import com.example.demo.model.Iso8583Message;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(2)
public class Iso8583Client implements CommandLineRunner {

    private static final int PORT = 8583;
    private Socket socket;
    private DataOutputStream output;
    private DataInputStream input;
    private final AtomicInteger stanCounter = new AtomicInteger(1);
    private final ScheduledExecutorService echoScheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean connected = false;
    private volatile Thread listenerThread;

    @Override
    public void run(String... args) throws Exception {
        String mode = System.getProperty("app.mode", "both");
        if ("server".equals(mode)) {
            return;
        }

        if ("client".equals(mode)) {
            runPersistentClient();
        } else {
            // Auto test mode
            Thread.sleep(3000);
            runAutoTests();
        }
    }

    private void runPersistentClient() {
        System.out.println("\n🔄 Persistent ISO 8583 Client");
        System.out.println("Commands:");
        System.out.println("  'connect'    - Connect to server");
        System.out.println("  'disconnect' - Disconnect from server");
        System.out.println("  'send'       - Send authorization request (0200)");
        System.out.println("  'echo'       - Send echo message (0800)");
        System.out.println("  'status'     - Show connection status");
        System.out.println("  'auto-echo'  - Toggle automatic echo messages");
        System.out.println("  'exit'       - Quit");

        Scanner scanner = new Scanner(System.in);
        boolean autoEcho = false;

        while (true) {
            System.out.print("\nClient" + (connected ? " [CONNECTED]" : " [DISCONNECTED]") + "> ");
            String command = scanner.nextLine().trim().toLowerCase();

            try {
                switch (command) {
                    case "connect":
                        connect();
                        break;

                    case "disconnect":
                        disconnect();
                        break;

                    case "send":
                        if (ensureConnected()) {
                            sendAuthorizationRequest();
                        }
                        break;

                    case "echo":
                        if (ensureConnected()) {
                            sendEchoMessage();
                        }
                        break;

                    case "status":
                        showConnectionStatus();
                        break;

                    case "auto-echo":
                        autoEcho = toggleAutoEcho(autoEcho);
                        break;

                    case "exit":
                    case "quit":
                        disconnect();
                        echoScheduler.shutdown();
                        System.out.println("👋 Goodbye!");
                        System.exit(0);
                        break;

                    default:
                        System.out.println("❓ Unknown command: " + command);
                        break;
                }
            } catch (Exception e) {
                System.err.println("❌ Error: " + e.getMessage());
            }
        }
    }

    private void connect() {
        try {
            if (connected) {
                System.out.println("ℹ️ Already connected!");
                return;
            }

            System.out.println("🔄 Connecting to server...");
            socket = new Socket("localhost", PORT);
            output = new DataOutputStream(socket.getOutputStream());
            input = new DataInputStream(socket.getInputStream());
            connected = true;

            System.out.println("✅ Connected successfully!");

            // Start the dedicated listener thread
            listenerThread = new Thread(this::listenForMessages);
            listenerThread.setDaemon(true);
            listenerThread.start();

            // Send initial echo to verify connection
            sendEchoMessage();

        } catch (Exception e) {
            System.err.println("❌ Connection failed: " + e.getMessage());
            connected = false;
        }
    }

    private void disconnect() {
        try {
            if (!connected) {
                System.out.println("ℹ️ Already disconnected!");
                return;
            }

            System.out.println("🔄 Disconnecting...");

            if (listenerThread != null) {
                listenerThread.interrupt();
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            connected = false;
            System.out.println("✅ Disconnected successfully!");

        } catch (Exception e) {
            System.err.println("❌ Disconnect error: " + e.getMessage());
        } finally {
            connected = false;
        }
    }

    private boolean ensureConnected() {
        if (!connected) {
            System.out.println("❌ Not connected! Use 'connect' command first.");
            return false;
        }
        return true;
    }

    // New listener method to handle all incoming messages
    private void listenForMessages() {
        try {
            while (!Thread.currentThread().isInterrupted() && connected) {
                int messageLength = input.readUnsignedShort();
                byte[] messageBytes = new byte[messageLength];
                input.readFully(messageBytes);
                String isoMessage = new String(messageBytes);

                processIncomingMessage(isoMessage);
            }
        } catch (EOFException e) {
            System.out.println("\n👋 Server disconnected gracefully.");
        } catch (SocketException e) {
            if (e.getMessage().contains("Socket closed")) {
                System.out.println("\n👋 Disconnected.");
            } else {
                System.err.println("\n🔌 Connection error in listener: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("\n🔌 IO error in listener: " + e.getMessage());
        } finally {
            connected = false;
            System.out.print("\nClient [DISCONNECTED]> ");
        }
    }

    private synchronized void sendAuthorizationRequest() throws Exception {
        Iso8583Message request = createAuthorizationRequest();
        String requestMessage = request.toString();
        System.out.println("📤 Sending Authorization: " + requestMessage);
        output.writeShort(requestMessage.length());
        output.writeBytes(requestMessage);
        output.flush();
        System.out.println("... Waiting for response...");
    }

    private synchronized void sendEchoMessage() throws Exception {
        Iso8583Message echoRequest = createEchoMessage();
        String requestMessage = echoRequest.toString();
        System.out.println("💓 Sending Echo: " + requestMessage);
        output.writeShort(requestMessage.length());
        output.writeBytes(requestMessage);
        output.flush();
        System.out.println("... Waiting for echo response...");
    }

    private void processIncomingMessage(String message) {
        System.out.println("\n📨 Received Message: " + message);

        Iso8583Message msg = parseMessage(message);
        String mti = msg.getMti();

        switch (mti) {
            case "0210": // Response to authorization
                printTransactionResult(message);
                break;
            case "0810": // Response to echo
                System.out.println("✅ Echo successful - Connection alive!");
                break;
            case "0200": // Unsolicited transaction from server
                System.out.println("➡️ Received unsolicited transaction from server.");
                // Here, you would process the server's transaction
                break;
            default:
                System.out.println("❓ Received unknown message type: " + mti);
                break;
        }
        // Reprompt the user for the next command
        System.out.print("\nClient" + (connected ? " [CONNECTED]" : " [DISCONNECTED]") + "> ");
    }


    private Iso8583Message createAuthorizationRequest() {
        Iso8583Message msg = new Iso8583Message();
        Random random = new Random();
        msg.setMti("0200");
        msg.addField(2, generatePan());
        msg.addField(3, "000000");
        msg.addField(4, String.format("%012d", random.nextInt(100000) + 1000));
        msg.addField(7, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss")));
        msg.addField(11, String.format("%06d", stanCounter.getAndIncrement()));
        msg.addField(12, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));
        msg.addField(13, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd")));
        msg.addField(18, "5999");
        msg.addField(22, "012");
        msg.addField(25, "00");
        msg.addField(37, String.format("%012d", random.nextInt(999999999)));
        msg.addField(41, "TERM001 ");
        msg.addField(42, "MERCHANT001     ");
        msg.addField(49, "840");
        return msg;
    }

    private Iso8583Message createEchoMessage() {
        Iso8583Message msg = new Iso8583Message();
        msg.setMti("0800");
        msg.addField(7, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss")));
        msg.addField(11, String.format("%06d", stanCounter.getAndIncrement()));
        msg.addField(70, "001");
        return msg;
    }

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

    private String generatePan() {
        Random random = new Random();
        return "4000" + String.format("%012d", random.nextInt(1000000000));
    }

    private void printTransactionResult(String response) {
        if (response.contains("39=00")) {
            System.out.println("✅ Transaction APPROVED");
            String approvalCode = extractField(response, "38");
            if (approvalCode != null) {
                System.out.println("   Approval Code: " + approvalCode);
            }
            String amount = extractField(response, "4");
            if (amount != null) {
                System.out.println("   Amount: $" + formatAmount(amount));
            }
        } else {
            System.out.println("❌ Transaction DECLINED");
            String responseCode = extractField(response, "39");
            if (responseCode != null) {
                System.out.println("   Response Code: " + responseCode);
            }
        }
    }

    private String extractField(String message, String fieldNumber) {
        String searchPattern = fieldNumber + "=";
        int startIndex = message.indexOf(searchPattern);
        if (startIndex == -1) return null;
        startIndex += searchPattern.length();
        int endIndex = message.indexOf("|", startIndex);
        if (endIndex == -1) endIndex = message.length();
        return message.substring(startIndex, endIndex);
    }

    private String formatAmount(String amount) {
        if (amount != null && amount.length() >= 3) {
            long cents = Long.parseLong(amount);
            return String.format("%.2f", cents / 100.0);
        }
        return "0.00";
    }

    private void showConnectionStatus() {
        System.out.println("\n📊 Connection Status:");
        System.out.println("   Connected: " + (connected ? "✅ Yes" : "❌ No"));
        if (connected && socket != null) {
            System.out.println("   Server: " + socket.getRemoteSocketAddress());
            System.out.println("   Local: " + socket.getLocalSocketAddress());
            System.out.println("   Socket Open: " + !socket.isClosed());
        }
        System.out.println("   STAN Counter: " + stanCounter.get());
    }

    private boolean toggleAutoEcho(boolean currentState) {
        if (currentState) {
            echoScheduler.shutdownNow();
            System.out.println("🔇 Automatic echo messages disabled");
            return false;
        } else {
            echoScheduler.scheduleAtFixedRate(() -> {
                if (connected) {
                    try {
                        System.out.println("\n⏰ Auto echo...");
                        sendEchoMessage();
                    } catch (Exception e) {
                        System.err.println("❌ Auto echo failed: " + e.getMessage());
                        connected = false;
                    }
                }
            }, 30, 30, TimeUnit.SECONDS);

            System.out.println("🔔 Automatic echo messages enabled (every 30 seconds)");
            return true;
        }
    }

    private void runAutoTests() throws Exception {
        System.out.println("\n🔄 Starting auto tests with persistent connection...");
        connect();
        if (!connected) {
            System.err.println("❌ Could not establish connection for auto tests");
            return;
        }
        try {
            sendEchoMessage();
            Thread.sleep(1000);
            for (int i = 1; i <= 3; i++) {
                System.out.println("\n--- Test Transaction " + i + " ---");
                sendAuthorizationRequest();
                Thread.sleep(1000);
            }
            sendEchoMessage();
            System.out.println("\n✅ All tests completed using persistent connection!");
        } finally {
            disconnect();
        }
    }
}
