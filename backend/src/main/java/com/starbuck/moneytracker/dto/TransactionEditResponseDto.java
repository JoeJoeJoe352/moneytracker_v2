package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.starbuck.moneytracker.entity.enums.TransactionSpecialTypeEnum;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;

public record TransactionEditResponseDto(
        Long id,
        String name,
        BigDecimal priceSum,
        LocalDate transactionDate,
        TransactionTypeEnum transactionType,
        boolean isComplexTransaction,
        List<TransactionDetailEditResponseDto> transactionDetails,
        Long walletId,
        TransactionSpecialTypeEnum specialType) {}
