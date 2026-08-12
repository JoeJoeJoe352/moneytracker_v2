package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;
import java.util.List;

public record TransactionDetailResponseDto(
    String name,
    BigDecimal price,
    BigDecimal weight,
    BigDecimal unitPrice,
    boolean isComplexPriceMode,
    List<Long> categories
) {
    
}
