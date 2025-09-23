package com.example.demo.parser;

import com.example.demo.model.Iso8583Message;

public class Iso8583Parser {
    public static Iso8583Message parseMessage(String message) {
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
}