package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;

public record MoneySumResponseDto(
    BigDecimal moneySum,
    BigDecimal incomeSumThisMonth,
    BigDecimal expenseSumThisMonth
) {

}
