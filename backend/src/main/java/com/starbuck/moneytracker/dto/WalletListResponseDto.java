package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.starbuck.moneytracker.entity.enums.CurrencyEnum;
import com.starbuck.moneytracker.entity.enums.WalletTypeEnum;

/**
 * @param lastSyncDate          az utolsó szinkronizálás napja a user időzónája szerint (ha még nem
 *                              volt, akkor a wallet létrehozásának napja)
 * @param recurringTransactions a wallethez tartozó (nem törölt) ismétlődő tranzakciók, a
 *                              legközelebb esedékes elöl
 */
public record WalletListResponseDto(
        Long id,
        String name,
        CurrencyEnum currencyCode,
        WalletTypeEnum type,
        BigDecimal sum,
        LocalDate lastSyncDate,
        boolean deletable,
        List<RecurringTransactionResponseDto> recurringTransactions) {}
