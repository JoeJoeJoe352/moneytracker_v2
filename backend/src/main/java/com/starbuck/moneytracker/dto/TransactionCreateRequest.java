package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.hibernate.validator.constraints.Length;

import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;
import com.starbuck.moneytracker.validation.PastOrPresentInZone;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransactionCreateRequest(
    @NotBlank(message = "Name is mandatory")
    @Length(min = 3, max = 120, message = "Name must be between 3 and 120 characters")
    String name,

    @Digits(integer = 8 , fraction = 2)
    BigDecimal globalPrice,

    @NotNull(message = "transactionType is mandatory")
    TransactionTypeEnum transactionType,

    @NotNull(message = "Date is mandatory")
    @PastOrPresentInZone
    LocalDate transactionDate,
    
    @Valid
    @NotNull(message = "TransactionDetails are mandatory")
    List<TransactionDetailCreateRequestDto> transactionDetails,

    @NotNull 
    List<Long> globalCategories,

    @NotNull
    @Positive
    Long walletId
) {}
