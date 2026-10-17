package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import com.starbuck.moneytracker.entity.enum_entites.CurrencyEnum;
import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;

/**
 * A wallet lista lekérdezés nyers eredménye (JPQL "SELECT new ..."). A dátumok még időpontok,
 * a user időzónája szerinti napot a WalletService számolja belőlük a WalletListResponseDto-ba
 *
 * @param lastSyncedAt az utolsó szinkronizálás időpontja, null ha még nem volt
 * @param createdAt    a wallet létrehozásának időpontja (a JVM időzónájában, @CreationTimestamp)
 */
public record WalletListRow(
        Long id,
        String name,
        CurrencyEnum currencyCode,
        WalletTypeEnum type,
        BigDecimal sum,
        Instant lastSyncedAt,
        LocalDateTime createdAt) {
}
