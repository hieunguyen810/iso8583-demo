package com.example.simulator.controller;

import com.example.simulator.config.SimulatorConfig;
import com.example.simulator.service.TransactionSimulatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {

    @Autowired
    private TransactionSimulatorService simulatorService;

    @Autowired
    private SimulatorConfig config;

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> sendSingleTransaction() {
        try {
            simulatorService.sendTransaction();
            return ResponseEntity.ok(Map.of("status", "success", "message", "Transaction sent"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/load-test/start")
    public ResponseEntity<Map<String, String>> startLoadTest() {
        if (config.getMode() != SimulatorConfig.Mode.MANUAL) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Switch to MANUAL mode first"));
        }
        
        try {
            simulatorService.startLoadTest();
            return ResponseEntity.ok(Map.of("status", "success", "message", "Load test started"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/spike-test/start")
    public ResponseEntity<Map<String, String>> startSpikeTest() {
        if (config.getMode() != SimulatorConfig.Mode.MANUAL) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Switch to MANUAL mode first"));
        }
        
        try {
            simulatorService.startSpikeTest();
            return ResponseEntity.ok(Map.of("status", "success", "message", "Spike test started"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @GetMapping("/config")
    public ResponseEntity<SimulatorConfig> getConfig() {
        return ResponseEntity.ok(config);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "enabled", config.isEnabled(),
            "mode", config.getMode(),
            "message", "Simulator is " + (config.isEnabled() ? "enabled" : "disabled")
        ));
    }
}