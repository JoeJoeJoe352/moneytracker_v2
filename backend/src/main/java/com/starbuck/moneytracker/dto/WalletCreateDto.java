package com.starbuck.moneytracker.dto;

import com.starbuck.moneytracker.entity.enum_entites.CurrencyEnum;
import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;

public record WalletCreateDto(
        String name,
        CurrencyEnum currencyCode,
        WalletTypeEnum walletType) {

}
