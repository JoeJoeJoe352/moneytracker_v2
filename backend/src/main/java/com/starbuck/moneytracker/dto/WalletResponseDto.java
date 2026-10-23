package com.starbuck.moneytracker.dto;

import com.starbuck.moneytracker.entity.enums.CurrencyEnum;
import com.starbuck.moneytracker.entity.enums.WalletTypeEnum;

public record WalletResponseDto(Long id, String name, CurrencyEnum currencyCode, WalletTypeEnum type) {}
