package com.example.server.repository;

import com.example.server.entity.Transaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "iso8583.database.write.enabled", havingValue = "true")
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}