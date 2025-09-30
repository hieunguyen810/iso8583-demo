package com.example.server.config;

import com.example.server.grpc.Iso8583ServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GrpcConfig {
    
    private Server server;
    
    @EventListener(ApplicationReadyEvent.class)
    public void startGrpcServer() {
        try {
            server = ServerBuilder.forPort(9090)
                    .addService(new Iso8583ServiceImpl())
                    .build()
                    .start();
            
            System.out.println("✅ gRPC Server started on port 9090");
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("🛑 Shutting down gRPC server");
                if (server != null) {
                    server.shutdown();
                }
            }));
            
        } catch (IOException e) {
            System.err.println("❌ Failed to start gRPC server: " + e.getMessage());
        }
    }
}