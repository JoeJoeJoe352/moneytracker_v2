package com.starbuck.moneytracker.dto;

import java.time.LocalDate;

import org.hibernate.validator.constraints.Length;

import com.starbuck.moneytracker.entity.TransactionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransactionCreateRequest (
    @NotBlank(message = "Name is mandatory")
    @Length(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    String name,

    @NotNull(message = "transactionType is mandatory")
    TransactionType transactionType,

    @NotNull(message = "Price is mandatory")
    @Positive(message = "Price must be greater than 0") 
    Float price,

    @NotNull(message = "Date is mandatory")
    LocalDate transactionDate
) {}
