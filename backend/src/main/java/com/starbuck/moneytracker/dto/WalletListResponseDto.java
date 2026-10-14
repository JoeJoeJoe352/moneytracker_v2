package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.starbuck.moneytracker.entity.enum_entites.CurrencyEnum;
import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;

public record WalletListResponseDto(
        Long id,
        String name,
        CurrencyEnum currencyCode,
        WalletTypeEnum type,
        BigDecimal sum,
        LocalDate lastSyncDate,
        boolean deletable
) {
    // constructor for the JPQL "SELECT new ..." query
    public WalletListResponseDto(Long id, String name, CurrencyEnum currencyCode,
        WalletTypeEnum type, BigDecimal sum, LocalDate lastSyncDate) {
        this(id, name, currencyCode, type, sum, lastSyncDate, false);
    }

    public WalletListResponseDto withDeletable(boolean deletable) {
        return new WalletListResponseDto(id, name, currencyCode, type, sum, lastSyncDate, deletable);
    }
} 
