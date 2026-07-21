package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotNull;

public record TransactionDetailCreateDto(
    @NotNull(message = "Price is mandatory")
    BigDecimal price,
    
    // Ez lehet null, ha simple tranzakcióról van szó. Todo ez nem fogja meg az egy karakteres nevet
    @Length(min = 3, max = 50, message = "Name must be between 3 and 50 characters")
    String name
){}
