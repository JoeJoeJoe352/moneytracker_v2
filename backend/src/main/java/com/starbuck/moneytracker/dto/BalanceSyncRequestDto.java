package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/**
 * @param currentBalance A user által megszámolt, valós egyenleg.
 */
public record BalanceSyncRequestDto(
        @NotNull 
        @Digits(integer = 8, fraction = 2) 
        BigDecimal currentBalance
) {
}
