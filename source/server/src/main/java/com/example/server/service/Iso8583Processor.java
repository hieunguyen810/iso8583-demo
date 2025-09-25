package com.example.server.service;

import com.example.common.model.Iso8583Message;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Iso8583Processor {
    public static Iso8583Message processMessage(Iso8583Message request) {
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
    private static void copyField(Iso8583Message source, Iso8583Message target, int fieldNumber) {
        String value = source.getField(fieldNumber);
        if (value != null) {
            target.addField(fieldNumber, value);
        }
    }
    private static String generateApprovalCode() {
            return String.format("%06d", (int) (Math.random() * 999999));
        }
}