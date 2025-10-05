package com.example.server.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class TransactionMetrics {
    
    private final Counter successfulTransactions;
    private final Counter failedTransactions;
    
    public TransactionMetrics(MeterRegistry meterRegistry) {
        this.successfulTransactions = Counter.builder("iso8583.transactions.successful")
                .description("Number of successful transactions (0200->0210)")
                .register(meterRegistry);
                
        this.failedTransactions = Counter.builder("iso8583.transactions.failed")
                .description("Number of failed transactions")
                .register(meterRegistry);
    }
    
    public void incrementSuccessful() {
        successfulTransactions.increment();
    }
    
    public void incrementFailed() {
        failedTransactions.increment();
    }
}