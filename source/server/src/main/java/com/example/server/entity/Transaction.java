package com.example.server.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "source_number", length = 20)
    private String sourceNumber;
    
    @Column(name = "target_number", length = 20)
    private String targetNumber;
    
    @Column(name = "status", length = 10)
    private String status;
    
    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "transaction_time")
    private LocalDateTime transactionTime;
    
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    @Column(name = "stan", length = 6)
    private String stan;
    
    @Column(name = "mti", length = 4)
    private String mti;

    public Transaction() {}

    public Transaction(String sourceNumber, String targetNumber, String status, 
                      BigDecimal amount, String stan, String mti) {
        this.sourceNumber = sourceNumber;
        this.targetNumber = targetNumber;
        this.status = status;
        this.amount = amount;
        this.stan = stan;
        this.mti = mti;
        this.transactionTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSourceNumber() { return sourceNumber; }
    public void setSourceNumber(String sourceNumber) { this.sourceNumber = sourceNumber; }
    
    public String getTargetNumber() { return targetNumber; }
    public void setTargetNumber(String targetNumber) { this.targetNumber = targetNumber; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { 
        this.status = status;
        this.updateTime = LocalDateTime.now();
    }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public LocalDateTime getTransactionTime() { return transactionTime; }
    public void setTransactionTime(LocalDateTime transactionTime) { this.transactionTime = transactionTime; }
    
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    
    public String getStan() { return stan; }
    public void setStan(String stan) { this.stan = stan; }
    
    public String getMti() { return mti; }
    public void setMti(String mti) { this.mti = mti; }
}