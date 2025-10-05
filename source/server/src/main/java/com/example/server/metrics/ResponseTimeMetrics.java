package com.example.server.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class ResponseTimeMetrics {
    
    private final Timer responseTimer;
    private final Counter timeoutCounter;
    private final Counter field37MismatchCounter;
    private final ConcurrentHashMap<String, Long> pendingRequests = new ConcurrentHashMap<>();
    
    public ResponseTimeMetrics(MeterRegistry meterRegistry) {
        this.responseTimer = Timer.builder("iso8583.response.time")
                .description("Response time for ISO 8583 transactions")
                .register(meterRegistry);
                
        this.timeoutCounter = Counter.builder("iso8583.response.timeout")
                .description("Number of transactions that timed out (>7 seconds)")
                .register(meterRegistry);
                
        this.field37MismatchCounter = Counter.builder("iso8583.field37.mismatch")
                .description("Number of responses with mismatched field 37")
                .register(meterRegistry);
    }
    
    public void startTimer(String field37) {
        pendingRequests.put(field37, System.currentTimeMillis());
    }
    
    public void recordResponse(String field37, boolean field37Matches) {
        Long startTime = pendingRequests.remove(field37);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            responseTimer.record(duration, TimeUnit.MILLISECONDS);
            
            if (duration > 7000) {
                timeoutCounter.increment();
                System.out.println("⏰ Transaction timeout: " + duration + "ms for field37: " + field37);
            }
            
            if (!field37Matches) {
                field37MismatchCounter.increment();
                System.out.println("❌ Field 37 mismatch for: " + field37);
            }
            
            System.out.println("📊 Response time: " + duration + "ms for field37: " + field37);
        }
    }
}