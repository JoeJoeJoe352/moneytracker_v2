package com.starbuck.moneytracker.dto;

import java.util.List;

public record UserDataResponseDto(
        long id,
        String username,
        List<WalletResponseDto> wallets) {
}
