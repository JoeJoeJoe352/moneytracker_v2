package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;
import java.util.List;

public record MoneySumResponseDto(
    List<WalletSummaryDto> moneySum,
    BigDecimal incomeSumThisMonth,
    BigDecimal expenseSumThisMonth
) {

}
