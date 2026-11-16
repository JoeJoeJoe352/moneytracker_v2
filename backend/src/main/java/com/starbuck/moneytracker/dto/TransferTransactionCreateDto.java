package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransferTransactionCreateDto(
        @NotBlank @Length(min = 3, max = 120, message = "srcTransactionName must be between 3 and 120 characters")
        String srcTransactionName,

        @NotNull long srcWalletId,

        @NotBlank @Length(min = 3, max = 120, message = "dstTransactionName must be between 3 and 120 characters")
        String dstTransactionName,

        @NotNull long dstWalletId,

        @NotNull LocalDate transactionDate,

        @NotNull @Digits(integer = 8, fraction = 2) BigDecimal priceSum) {}
