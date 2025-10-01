package com.example.client.controller;

import com.example.client.model.ApiResponse;
import com.example.client.model.ConnectionInfo;
import com.example.client.service.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/iso8583")
@CrossOrigin(origins = "*")
public class Iso8583Controller {

    @Autowired
    private ConnectionService connectionService;

    @GetMapping("/connections")
    public List<ConnectionInfo> getConnections() {
        return connectionService.getAllConnections();
    }

    @PostMapping("/connections")
    public ApiResponse addConnection(@RequestBody ConnectionInfo connectionInfo) {
        try {
            connectionService.addConnection(connectionInfo);
            return new ApiResponse(true, "Connection added successfully");
        } catch (Exception e) {
            return new ApiResponse(false, e.getMessage());
        }
    }

    @PostMapping("/connections/{connectionId}/connect")
    public ApiResponse connect(@PathVariable String connectionId) {
        try {
            connectionService.connect(connectionId);
            return new ApiResponse(true, "Connected successfully");
        } catch (Exception e) {
            return new ApiResponse(false, e.getMessage());
        }
    }

    @PostMapping("/connections/{connectionId}/disconnect")
    public ApiResponse disconnect(@PathVariable String connectionId) {
        try {
            connectionService.disconnect(connectionId);
            return new ApiResponse(true, "Disconnected successfully");
        } catch (Exception e) {
            return new ApiResponse(false, e.getMessage());
        }
    }

    @DeleteMapping("/connections/{connectionId}")
    public ApiResponse removeConnection(@PathVariable String connectionId) {
        try {
            connectionService.removeConnection(connectionId);
            return new ApiResponse(true, "Connection removed successfully");
        } catch (Exception e) {
            return new ApiResponse(false, e.getMessage());
        }
    }

    @PostMapping("/connections/{connectionId}/echo")
    public ApiResponse sendEcho(@PathVariable String connectionId) {
        try {
            String[] result = connectionService.sendEcho(connectionId);
            return new ApiResponse(true, "Echo sent successfully", result[0], result[1]);
        } catch (Exception e) {
            return new ApiResponse(false, e.getMessage());
        }
    }

    @PostMapping("/connections/{connectionId}/send")
    public ApiResponse sendMessage(@PathVariable String connectionId, @RequestBody Map<String, String> payload) {
        try {
            String message = payload.get("message");
            String[] result = connectionService.sendMessage(connectionId, message);
            return new ApiResponse(true, "Message sent successfully", result[0], result[1]);
        } catch (Exception e) {
            return new ApiResponse(false, e.getMessage());
        }
    }
}