package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import com.starbuck.moneytracker.entity.TransactionTypeEnum;

public record TransactionResponseDto(
    Long id,
    String name,
    BigDecimal priceSum,
    LocalDate transactionDate,
    TransactionTypeEnum transactionType,
    boolean isComplexTransaction,
    Set<TransactionDetailResponseDto> transactionDetails
) {
}
