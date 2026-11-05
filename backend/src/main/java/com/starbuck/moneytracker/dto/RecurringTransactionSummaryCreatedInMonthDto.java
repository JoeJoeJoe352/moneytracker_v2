package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;

import com.starbuck.moneytracker.entity.enums.CurrencyEnum;

public record RecurringTransactionSummaryCreatedInMonthDto(
        long recurringTransactionId, BigDecimal sum, String name, CurrencyEnum currencyCode) {}
