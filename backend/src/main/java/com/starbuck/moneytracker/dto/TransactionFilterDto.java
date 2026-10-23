package com.starbuck.moneytracker.dto;

import java.time.LocalDate;

/**
 * Keresési feltételek struktúrája
 */
public record TransactionFilterDto(
        /**
         * Tranzakció név
         */
        String name,
        /**
         * Tranzakció dátuma. Pl.: 2026-06-25
         */
        LocalDate date,
        /**
         * Tranzakció id
         */
        Long transactionId) {}
