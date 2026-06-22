package com.starbuck.moneytracker.dto;

import java.time.LocalDate;

import com.starbuck.moneytracker.entity.TransactionType;

public class TransactionDto {
    private Long id;
    private String name;
    private float priceSum;
    private LocalDate transactionDate;
    private TransactionType transactionType;

    public TransactionDto(Long id, String name, float priceSum, LocalDate transactionDate,
            TransactionType transactionType) {
        this.id = id;
        this.name = name;
        this.priceSum = priceSum;
        this.transactionDate = transactionDate;
        this.transactionType = transactionType;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public float getPriceSum() {
        return priceSum;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

}
