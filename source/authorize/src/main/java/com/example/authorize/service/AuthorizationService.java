package com.example.authorize.service;

import com.example.common.model.Iso8583Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Service
public class AuthorizationService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    public AuthorizationService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(topics = "iso8583-requests", groupId = "authorize-service")
    public void processAuthorizationRequest(String message) {
        try {
            System.out.println("📥 Received authorization request: " + message);
            
            JsonNode jsonMessage = objectMapper.readTree(message);
            String mti = jsonMessage.get("messageType").asText();
            
            if ("0200".equals(mti)) {
                Iso8583Message response = createAuthorizationResponse(jsonMessage);
                String responseMessage = response.toString();
                
                System.out.println("📤 Sending authorization response: " + responseMessage);
                kafkaTemplate.send("iso8583-responses", responseMessage);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error processing authorization: " + e.getMessage());
        }
    }

    private Iso8583Message createAuthorizationResponse(JsonNode request) {
        Iso8583Message response = new Iso8583Message();
        response.setMti("0210");
        
        JsonNode fields = request.get("fields");
        
        // Copy key fields from request
        if (fields.has("2")) response.addField(2, fields.get("2").get("value").asText());
        if (fields.has("3")) response.addField(3, fields.get("3").get("value").asText());
        if (fields.has("4")) response.addField(4, fields.get("4").get("value").asText());
        if (fields.has("11")) response.addField(11, fields.get("11").get("value").asText());
        if (fields.has("37")) response.addField(37, fields.get("37").get("value").asText());
        
        // Add response fields
        response.addField(7, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss")));
        response.addField(38, generateApprovalCode());
        response.addField(39, "00"); // Approved
        
        return response;
    }

    private String generateApprovalCode() {
        return String.format("%06d", random.nextInt(999999));
    }
}