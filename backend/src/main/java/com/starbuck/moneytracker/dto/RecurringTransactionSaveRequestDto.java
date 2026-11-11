package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.hibernate.validator.constraints.Length;

import com.starbuck.moneytracker.entity.enums.RecurrenceIntervalEnum;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;
import com.starbuck.moneytracker.validation.NotBeforeCurrentMonthInZone;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RecurringTransactionSaveRequestDto(
        @NotBlank(message = "Name is mandatory")
        @Length(min = 3, max = 120, message = "Name must be between 3 and 120 characters")
        String name,

        @NotNull(message = "priceSum is mandatory") @Digits(integer = 8, fraction = 2)
        BigDecimal priceSum,

        // a hónapon belül korábbi kezdés is megengedett: a scheduler a már esedékes
        // előfordulásokat utólag létrehozza
        @NotNull(message = "startDate is mandatory") @NotBeforeCurrentMonthInZone
        LocalDate startDate,

        @NotNull(message = "recurrenceInterval is mandatory")
        RecurrenceIntervalEnum recurrenceInterval,

        @NotNull(message = "transactionType is mandatory") TransactionTypeEnum transactionType,

        @NotNull List<Long> categoryIds,

        @NotNull @Positive Long walletId,

        @NotNull boolean isRestart) {}
