package com.example.demo.simulator;

import com.example.demo.model.Iso8583Message;

public class Iso8583TransactionSimulator {
    public static Iso8583Message getTransaction() {
        Iso8583Message transaction = new Iso8583Message();
        transaction.setMti("0200");
        transaction.addField(2, "0000000000000000");
        return transaction;
    }
}