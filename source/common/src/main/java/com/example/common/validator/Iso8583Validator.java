package com.example.common.validator;

import com.example.common.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Iso8583Validator {
    private Map<Integer, FieldRule> fieldRules;
    private Map<String, MtiRule> mtiRules;

    public Iso8583Validator() {
        loadRules();
    }

    private void loadRules() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getClassLoader().getResourceAsStream("iso8583-rules.json");
            JsonNode root = mapper.readTree(is);

            fieldRules = new HashMap<>();
            JsonNode fields = root.get("fields");
            fields.fieldNames().forEachRemaining(fieldNum -> {
                FieldRule rule = mapper.convertValue(fields.get(fieldNum), FieldRule.class);
                fieldRules.put(Integer.parseInt(fieldNum), rule);
            });

            mtiRules = new HashMap<>();
            JsonNode mtis = root.get("mtiRules");
            mtis.fieldNames().forEachRemaining(mti -> {
                MtiRule rule = mapper.convertValue(mtis.get(mti), MtiRule.class);
                mtiRules.put(mti, rule);
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to load ISO 8583 rules", e);
        }
    }

    public ValidationResult validate(Iso8583Message message) {
        ValidationResult result = new ValidationResult();

        // Validate MTI
        String mti = message.getMti();
        if (mti == null || mti.length() != 4) {
            result.addError("Invalid MTI format");
            return result;
        }

        MtiRule mtiRule = mtiRules.get(mti);
        if (mtiRule == null) {
            result.addError("Unknown MTI: " + mti);
            return result;
        }

        // Check required fields
        for (Integer requiredField : mtiRule.getRequiredFields()) {
            if (!message.getFields().containsKey(requiredField)) {
                result.addError("Missing required field: " + requiredField);
            }
        }

        // Validate field formats
        for (Map.Entry<Integer, String> entry : message.getFields().entrySet()) {
            Integer fieldNum = entry.getKey();
            String value = entry.getValue();
            FieldRule rule = fieldRules.get(fieldNum);

            if (rule != null) {
                validateField(fieldNum, value, rule, result);
            }
        }

        return result;
    }

    private void validateField(Integer fieldNum, String value, FieldRule rule, ValidationResult result) {
        // Validate type
        if ("NUMERIC".equals(rule.getType()) && !value.matches("\\d+")) {
            result.addError("Field " + fieldNum + " must be numeric");
        }

        // Validate length
        if ("FIXED".equals(rule.getFormat()) && rule.getLength() != null) {
            if (value.length() != rule.getLength()) {
                result.addError("Field " + fieldNum + " must be exactly " + rule.getLength() + " characters");
            }
        }

        if (rule.getMaxLength() != null && value.length() > rule.getMaxLength()) {
            result.addError("Field " + fieldNum + " exceeds maximum length of " + rule.getMaxLength());
        }
    }
}