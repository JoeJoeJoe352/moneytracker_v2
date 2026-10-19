package com.starbuck.moneytracker.dto;

import java.util.List;

public record MoneySumResponseDto(
    List<WalletSummaryResponseDto> moneySum,
    List<WalletSummaryResponseDto> incomeSumThisMonth,
    List<WalletSummaryResponseDto> expenseSumThisMonth
) {

}
