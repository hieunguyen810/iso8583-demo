package com.example.server.grpc;

import com.example.simulator.grpc.Iso8583Proto;
import com.example.simulator.grpc.Iso8583ServiceGrpc;
import com.example.server.server.Iso8583Server;
import com.example.common.model.Iso8583Message;
import com.example.common.model.ValidationResult;
import com.example.common.parser.Iso8583Parser;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class Iso8583ServiceImpl extends Iso8583ServiceGrpc.Iso8583ServiceImplBase {
    
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
            
            // Send message to all connected socket clients
            Iso8583Server.broadcastToClients(message);
            
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
}