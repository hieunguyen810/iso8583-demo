package com.example.server.grpc;

import com.example.simulator.grpc.Iso8583Proto;
import com.example.simulator.grpc.Iso8583ServiceGrpc;
import com.example.server.server.Iso8583Server;
import com.example.common.model.Iso8583Message;
import com.example.common.model.ValidationResult;
import com.example.common.parser.Iso8583Parser;
import com.example.server.entity.Transaction;
import com.example.server.entity.TransactionEvent;
import com.example.server.repository.TransactionRepository;
import com.example.server.repository.TransactionEventRepository;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class Iso8583ServiceImpl extends Iso8583ServiceGrpc.Iso8583ServiceImplBase {
    
    @Autowired(required = false)
    private TransactionRepository transactionRepository;
    
    @Autowired(required = false)
    private TransactionEventRepository eventRepository;
    
    @Value("${iso8583.database.write.enabled:true}")
    private boolean databaseWriteEnabled;
    
    public Iso8583ServiceImpl() {
        System.out.println("✅ Iso8583ServiceImpl created");
    }

    @Override
    public void sendTransaction(Iso8583Proto.TransactionRequest request, 
                               StreamObserver<Iso8583Proto.TransactionResponse> responseObserver) {
        try {
            String message = request.getMessage();
            String clientId = request.getClientId();
            
            System.out.println("📥 gRPC received from " + clientId + ": " + message);
            
            // Parse and validate message
            Iso8583Message parsedMsg = Iso8583Parser.parseMessage(message);
            ValidationResult validation = Iso8583Parser.validateMessage(parsedMsg);
            
            if (!validation.isValid()) {
                throw new RuntimeException("Invalid message: " + String.join(", ", validation.getErrors()));
            }
            
            // Save transaction to database if enabled
            Long transactionId = null;
            if (databaseWriteEnabled && transactionRepository != null && eventRepository != null) {
                Transaction transaction = saveTransaction(parsedMsg);
                transactionId = transaction.getId();
                saveTransactionEvent(transactionId, "RECEIVED", message);
            }
            
            // Send message to all connected socket clients
            Iso8583Server.broadcastToClients(message);
            
            // Log broadcast event if database enabled
            if (databaseWriteEnabled && transactionId != null && eventRepository != null) {
                saveTransactionEvent(transactionId, "BROADCAST", message);
            }
            
            Iso8583Proto.TransactionResponse response = Iso8583Proto.TransactionResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Transaction sent to clients")
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            System.err.println("❌ gRPC error: " + e.getMessage());
            
            Iso8583Proto.TransactionResponse response = Iso8583Proto.TransactionResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error: " + e.getMessage())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
    
    private Transaction saveTransaction(Iso8583Message msg) {
        String sourceNumber = msg.getField(2); // PAN
        String targetNumber = msg.getField(42); // Card acceptor ID
        String amountStr = msg.getField(4); // Transaction amount
        String stan = msg.getField(11); // STAN
        String mti = msg.getMti();
        
        BigDecimal amount = BigDecimal.ZERO;
        if (amountStr != null && !amountStr.isEmpty()) {
            try {
                amount = new BigDecimal(amountStr).divide(new BigDecimal(100)); // Convert from cents
            } catch (NumberFormatException e) {
                System.err.println("Invalid amount format: " + amountStr);
            }
        }
        
        Transaction transaction = new Transaction(
            sourceNumber != null ? sourceNumber : "UNKNOWN",
            targetNumber != null ? targetNumber : "UNKNOWN",
            "RECEIVED",
            amount,
            stan,
            mti
        );
        
        return transactionRepository.save(transaction);
    }
    
    private void saveTransactionEvent(Long transactionId, String eventType, String isoMessage) {
        TransactionEvent event = new TransactionEvent(transactionId, eventType, isoMessage);
        eventRepository.save(event);
    }
}