package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/**
 * @param currentBalance A user által megszámolt, valós egyenleg. Max 2
 *                       tizedesjegy, hogy kerekítés után se jöhessen létre
 *                       0 összegű korrekció, és beférjen a DECIMAL(10,2)
 *                       oszlopba
 */
public record BalanceSyncDto(
        @NotNull 
        @Digits(integer = 8, fraction = 2) 
        BigDecimal currentBalance
) {
}
