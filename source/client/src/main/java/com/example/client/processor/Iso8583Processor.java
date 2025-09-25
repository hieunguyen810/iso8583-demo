package com.example.client.processor;

import com.example.common.model.Iso8583Message;
import com.example.common.parser.Iso8583Parser;

public class Iso8583Processor {
    public static void processIncomingMessage(String message, boolean connected) {
        System.out.println("\n📨 Received Message: " + message);

        Iso8583Message msg = Iso8583Parser.parseMessage(message);
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
    private static void printTransactionResult(String response) {
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
    private static String extractField(String message, String fieldNumber) {
        String searchPattern = fieldNumber + "=";
        int startIndex = message.indexOf(searchPattern);
        if (startIndex == -1) return null;
        startIndex += searchPattern.length();
        int endIndex = message.indexOf("|", startIndex);
        if (endIndex == -1) endIndex = message.length();
        return message.substring(startIndex, endIndex);
    }

    private static String formatAmount(String amount) {
        if (amount != null && amount.length() >= 3) {
            long cents = Long.parseLong(amount);
            return String.format("%.2f", cents / 100.0);
        }
        return "0.00";
    }
}