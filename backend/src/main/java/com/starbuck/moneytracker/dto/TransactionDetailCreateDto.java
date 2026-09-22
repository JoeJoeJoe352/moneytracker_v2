package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.validator.constraints.Length;

import com.starbuck.moneytracker.validation.TransactionDetailAttributeCoexist;

@TransactionDetailAttributeCoexist
public record TransactionDetailCreateDto(

    BigDecimal price,

    @Length(min = 3, max = 20, message = "Name must be between 3 and 20 characters") 
    String name,

    BigDecimal weight,

    BigDecimal unitPrice,

    List<Long> categories
) {
}
