package com.starbuck.moneytracker.entity;

import java.time.LocalDate;

/**
 * Keresési feltételek struktúrája
 */
public record TransactionFilter(
    /**
     * Tranzakció név
     */
    String name,
    /**
     * Tranzakció dátuma. Pl.: 2026-06-25
     */
    LocalDate dateString
) {}
