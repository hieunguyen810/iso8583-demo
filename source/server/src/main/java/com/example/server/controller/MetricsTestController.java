package com.example.server.controller;

import com.example.server.metrics.TransactionMetrics;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricsTestController {
    
    private final TransactionMetrics transactionMetrics;
    
    public MetricsTestController(TransactionMetrics transactionMetrics) {
        this.transactionMetrics = transactionMetrics;
    }
    
    @PostMapping("/test/success")
    public String testSuccess() {
        transactionMetrics.incrementSuccessful();
        return "Success metric incremented";
    }
    
    @PostMapping("/test/failed")
    public String testFailed() {
        transactionMetrics.incrementFailed();
        return "Failed metric incremented";
    }
}