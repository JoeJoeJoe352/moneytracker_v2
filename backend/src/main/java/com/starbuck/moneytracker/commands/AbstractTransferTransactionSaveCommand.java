package com.starbuck.moneytracker.commands;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Átvezetés (TRANSFER) létrehozásának és módosításának közös adatai. Az összeg
 * mindig pozitív: a forrás oldalon kiadásként, a cél oldalon bevételként kerül
 * mentésre
 */
public abstract class AbstractTransferTransactionSaveCommand {

    protected String srcTransactionName;
    protected String dstTransactionName;
    protected LocalDate transactionDate;
    protected BigDecimal priceSum;

    /**
     * @param srcTransactionName A forrás (kiadás) oldali tranzakció neve
     * @param dstTransactionName A cél (bevétel) oldali tranzakció neve
     * @param transactionDate    Az átvezetés dátuma (mindkét oldalon ugyanaz)
     * @param priceSum           Az átvezetett összeg (pozitív)
     */
    protected AbstractTransferTransactionSaveCommand(
            String srcTransactionName, String dstTransactionName, LocalDate transactionDate, BigDecimal priceSum) {

        if (srcTransactionName == null || srcTransactionName.isBlank()) {
            throw new IllegalArgumentException("Source transaction name cannot be empty");
        }
        if (dstTransactionName == null || dstTransactionName.isBlank()) {
            throw new IllegalArgumentException("Destination transaction name cannot be empty");
        }
        if (transactionDate == null) {
            throw new IllegalArgumentException("Transaction date cannot be null");
        }
        if (priceSum == null) {
            throw new IllegalArgumentException("Price sum cannot be null");
        }
        if (priceSum.signum() <= 0) {
            throw new IllegalArgumentException("Price sum of a transfer must be positive");
        }

        this.srcTransactionName = srcTransactionName;
        this.dstTransactionName = dstTransactionName;
        this.transactionDate = transactionDate;
        this.priceSum = priceSum;
    }

    public String getSrcTransactionName() {
        return srcTransactionName;
    }

    public String getDstTransactionName() {
        return dstTransactionName;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public BigDecimal getPriceSum() {
        return priceSum;
    }
}
