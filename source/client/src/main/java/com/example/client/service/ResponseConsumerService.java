package com.example.client.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Service
@ConditionalOnProperty(name = "iso8583.client.authorization.enabled", havingValue = "true")
public class ResponseConsumerService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 8583;

    @KafkaListener(topics = "iso8583-responses", groupId = "client-response-consumer")
    public void consumeResponse(String message) {
        try {
            System.out.println("📥 Received response from Kafka: " + message);
            
            // Check if message is JSON or raw ISO message
            String rawMessage;
            if (message.startsWith("{")) {
                // JSON format
                JsonNode jsonMessage = objectMapper.readTree(message);
                rawMessage = jsonMessage.get("rawMessage").asText();
            } else {
                // Raw ISO message format
                rawMessage = message;
            }
            
            sendToSocketServer(rawMessage);
            
        } catch (Exception e) {
            System.err.println("❌ Error processing response: " + e.getMessage());
        }
    }

    private void sendToSocketServer(String message) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {
            
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            output.writeShort(messageBytes.length);
            output.write(messageBytes);
            output.flush();
            
            System.out.println("📤 Sent response to server: " + message);
            
        } catch (Exception e) {
            System.err.println("❌ Failed to send to server: " + e.getMessage());
        }
    }
}