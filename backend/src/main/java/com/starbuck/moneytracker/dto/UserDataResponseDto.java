package com.starbuck.moneytracker.dto;

import java.util.List;

public record UserDataResponseDto(
        String username,
        List<WalletResponseDto> wallets) {
}
