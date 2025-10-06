package com.example.simulator.service;

import com.example.common.model.Iso8583Message;
import com.example.simulator.grpc.Iso8583Proto;
import com.example.simulator.grpc.Iso8583ServiceGrpc;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

@Service
public class TransactionSimulatorService {

    @GrpcClient("iso8583-server")
    private Iso8583ServiceGrpc.Iso8583ServiceBlockingStub iso8583ServiceStub;

    private final Random random = new Random();
    private final AtomicInteger stanCounter = new AtomicInteger(1);

    @Scheduled(fixedRate = 15000) // Every 15 seconds
    public void sendRandomTransaction() {
        int maxRetries = 3;
        int retryDelay = 2000; // 2 seconds
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Iso8583Message transaction = createRandomTransaction();
                String message = transaction.toString();
                
                System.out.println("📤 Sending transaction (attempt " + attempt + "): " + message);
                
                Iso8583Proto.TransactionRequest request = Iso8583Proto.TransactionRequest.newBuilder()
                        .setMessage(message)
                        .setClientId("simulator-" + System.currentTimeMillis())
                        .build();
                
                Iso8583Proto.TransactionResponse response = iso8583ServiceStub
                        .withDeadlineAfter(5, TimeUnit.SECONDS)
                        .sendTransaction(request);
                
                if (response.getSuccess()) {
                    System.out.println("✅ Transaction sent successfully");
                } else {
                    System.err.println("❌ Transaction failed: " + response.getMessage());
                }
                return; // Success, exit retry loop
                
            } catch (StatusRuntimeException e) {
                System.err.println("❌ gRPC error (attempt " + attempt + "): " + e.getStatus());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Error sending transaction (attempt " + attempt + "): " + e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
        System.err.println("❌ Failed to send transaction after " + maxRetries + " attempts");
    }

    private Iso8583Message createRandomTransaction() {
        Iso8583Message msg = new Iso8583Message();
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
        msg.addField(41, "SIM001  ");
        msg.addField(42, "SIMULATOR000001");
        msg.addField(49, "840");
        
        return msg;
    }

    private String generatePan() {
        return "4000" + String.format("%012d", random.nextInt(1000000000));
    }
}