package com.example.server.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_events")
public class TransactionEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transaction_id")
    private Long transactionId;
    
    @Column(name = "event_type", length = 20)
    private String eventType;
    
    @Column(name = "iso_message", columnDefinition = "TEXT")
    private String isoMessage;
    
    @Column(name = "event_time")
    private LocalDateTime eventTime;

    public TransactionEvent() {}

    public TransactionEvent(Long transactionId, String eventType, String isoMessage) {
        this.transactionId = transactionId;
        this.eventType = eventType;
        this.isoMessage = isoMessage;
        this.eventTime = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public String getIsoMessage() { return isoMessage; }
    public void setIsoMessage(String isoMessage) { this.isoMessage = isoMessage; }
    
    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
}