package com.example.common.parser;

import com.example.common.model.Iso8583Message;
import com.example.common.model.ValidationResult;
import com.example.common.validator.Iso8583Validator;

public class Iso8583Parser {
    private static final Iso8583Validator validator = new Iso8583Validator();

    public static Iso8583Message parseMessage(String message) {
        Iso8583Message msg = new Iso8583Message();
        
        if (message.startsWith("MTI=")) {
            String[] parts = message.split("\\|");
            for (String part : parts) {
                String[] fieldValue = part.split("=", 2);
                if (fieldValue.length == 2) {
                    if ("MTI".equals(fieldValue[0])) {
                        msg.setMti(fieldValue[1]);
                    } else if (fieldValue[0].startsWith("F")) {
                        try {
                            int fieldNumber = Integer.parseInt(fieldValue[0].substring(1));
                            msg.addField(fieldNumber, fieldValue[1]);
                        } catch (NumberFormatException e) {
                            System.err.println("⚠️ Invalid field number: " + fieldValue[0]);
                        }
                    }
                }
            }
        } else if (message.length() >= 4) {
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

    public static ValidationResult validateMessage(Iso8583Message message) {
        return validator.validate(message);
    }
}