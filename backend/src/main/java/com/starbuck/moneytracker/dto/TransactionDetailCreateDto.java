package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransactionDetailCreateDto(
    @NotNull(message = "Price is mandatory")
    BigDecimal price,
    
    @NotBlank(message = "Name is mandatory")
    @Length(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    String name
){}
