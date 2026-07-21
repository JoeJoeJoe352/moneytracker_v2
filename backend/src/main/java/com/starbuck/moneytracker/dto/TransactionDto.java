package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.starbuck.moneytracker.entity.TransactionTypeEnum;

public record TransactionDto(
                Long id,
                String name,
                BigDecimal priceSum,
                LocalDate transactionDate,
                TransactionTypeEnum transactionType) {
}
