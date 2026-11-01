package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.starbuck.moneytracker.entity.enums.RecurrenceIntervalEnum;
import com.starbuck.moneytracker.entity.enums.RecurringTransactionStatusEnum;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;

public record RecurringTransactionResponseDto(
        Long id,
        String name,
        BigDecimal priceSum,
        LocalDate startDate,
        LocalDate nextOccurrenceDate,
        RecurrenceIntervalEnum recurrenceInterval,
        TransactionTypeEnum transactionType,
        RecurringTransactionStatusEnum status,
        List<CategoryResponseDto> categories,
        WalletResponseDto wallet) {}
