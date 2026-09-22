package com.starbuck.moneytracker.dto;

import org.hibernate.validator.constraints.Length;

import com.starbuck.moneytracker.entity.enum_entites.CurrencyEnum;
import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;

import jakarta.validation.constraints.NotBlank;

public record WalletCreateDto(
        @NotBlank
        @Length(min = 3, max = 20, message = "Wallet name must be between 3 and 20 characters")
        String name,

        CurrencyEnum currencyCode,

        WalletTypeEnum walletType) {

}
