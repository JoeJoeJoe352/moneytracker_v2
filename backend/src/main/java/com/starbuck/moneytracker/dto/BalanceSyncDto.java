package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

public record BalanceSyncDto(
        @NotNull 
        Long walletId,

        @NotNull 
        @Digits(integer = 8, fraction = 2) BigDecimal currentBalance,

        @NotNull 
        @PastOrPresent LocalDate syncDate) {

}
