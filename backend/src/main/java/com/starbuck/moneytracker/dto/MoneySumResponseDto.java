package com.starbuck.moneytracker.dto;

import java.util.List;

public record MoneySumResponseDto(
    List<WalletSummaryDto> moneySum,
    List<WalletSummaryDto> incomeSumThisMonth,
    List<WalletSummaryDto> expenseSumThisMonth
) {

}
