package com.example.server.service;

import com.example.server.metrics.TransactionMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class TransactionTimer {
    
    private final TransactionMetrics transactionMetrics;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final ConcurrentHashMap<String, String> pendingTransactions = new ConcurrentHashMap<>();
    
    @Value("${iso8583.transaction.timeout:7}")
    private int timeoutSeconds;
    
    public TransactionTimer(TransactionMetrics transactionMetrics) {
        this.transactionMetrics = transactionMetrics;
    }
    
    public void startTimer(String field37) {
        pendingTransactions.put(field37, field37);
        scheduler.schedule(() -> {
            if (pendingTransactions.remove(field37) != null) {
                System.out.println("⏰ Transaction timeout for field37: " + field37);
                transactionMetrics.incrementFailed();
            }
        }, timeoutSeconds, TimeUnit.SECONDS);
    }
    
    public void checkResponse(String field37) {
        if (pendingTransactions.remove(field37) != null) {
            System.out.println("✅ Transaction successful for field37: " + field37);
            transactionMetrics.incrementSuccessful();
        }
    }
}