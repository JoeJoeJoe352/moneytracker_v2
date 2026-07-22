package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;

public record TransactionDetailDto(
    String name,
    BigDecimal price,
    BigDecimal weight,
    BigDecimal unitPrice
) {
    
}
