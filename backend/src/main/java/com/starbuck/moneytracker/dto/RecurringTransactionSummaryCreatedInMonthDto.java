package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.starbuck.moneytracker.entity.enums.CurrencyEnum;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;

public record RecurringTransactionSummaryCreatedInMonthDto(
        long recurringTransactionId,
        String name,
        BigDecimal recurringTransactionPrice,
        LocalDate nextOccurrenceDate,
        TransactionTypeEnum transactionType,
        // a hónapban már létrejött tranzakciók összege
        BigDecimal createdSum,
        // a hónap végéig még tervezett tranzakciók összege
        BigDecimal plannedSum,
        CurrencyEnum currencyCode) {}
