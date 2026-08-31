package com.starbuck.moneytracker.dto;

import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;

public record WalletUpdateDto(
        String name,
        WalletTypeEnum walletType) {

}
