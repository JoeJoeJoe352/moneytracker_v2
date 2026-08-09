package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;
import java.util.ArrayList;

public record TransactionDetailResponseDto(
    String name,
    BigDecimal price,
    BigDecimal weight,
    BigDecimal unitPrice,
    boolean isComplexPriceMode,
    ArrayList<Integer> categories
) {
    
}
