package com.starbuck.moneytracker.dto;

import java.time.LocalDate;

import com.starbuck.moneytracker.entity.TransactionType;

public record TransactionDto(
        Long id,
        String name,
        double priceSum,
        LocalDate transactionDate,
        TransactionType transactionType) {
}
