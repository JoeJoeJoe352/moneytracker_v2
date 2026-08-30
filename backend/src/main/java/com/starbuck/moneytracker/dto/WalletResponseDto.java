package com.starbuck.moneytracker.dto;

import com.starbuck.moneytracker.entity.enum_entites.CurrencyEnum;
import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;

public record WalletResponseDto(
        Long id,
        String name,
        CurrencyEnum currencyCode,
        WalletTypeEnum type) {
}
