package com.example.server.repository;

import com.example.server.entity.TransactionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionEventRepository extends JpaRepository<TransactionEvent, Long> {
}