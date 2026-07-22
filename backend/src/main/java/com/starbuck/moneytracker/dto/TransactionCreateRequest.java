package com.starbuck.moneytracker.dto;

import java.time.LocalDate;
import java.util.List;

import org.hibernate.validator.constraints.Length;

import com.starbuck.moneytracker.entity.TransactionTypeEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransactionCreateRequest(
    @NotBlank(message = "Name is mandatory")
    @Length(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    String name,
            
    @NotNull(message = "transactionType is mandatory")
    TransactionTypeEnum transactionType,

    @NotNull(message = "Date is mandatory") 
    LocalDate transactionDate,
    
    @NotNull(message = "TransactionDetails are mandatory")
    List<TransactionDetailCreateDto> transactionDetails
) {}
