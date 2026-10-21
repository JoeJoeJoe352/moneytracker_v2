package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.validator.constraints.Length;

import com.starbuck.moneytracker.validation.TransactionDetailAttributeCoexist;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

@TransactionDetailAttributeCoexist
public record TransactionDetailCreateRequestDto(
        @Digits(integer = 8, fraction = 2) BigDecimal price,

        @Length(min = 3, max = 20, message = "Name must be between 3 and 20 characters")
        String name,

        @Digits(integer = 8, fraction = 2) BigDecimal weight,

        @Digits(integer = 8, fraction = 2) BigDecimal unitPrice,

        @NotNull List<Long> categories) {}
