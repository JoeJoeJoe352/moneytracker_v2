package com.starbuck.moneytracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Egy átvezetés adatai a szerkesztéshez
 *
 * @param transferId         az átvezetés (TransferTransactionSide) id-ja, ezzel módosítható és
 *                           törölhető
 * @param srcTransactionName a forrás (kiadás) oldali tranzakció neve
 * @param srcWalletId        a forrás wallet
 * @param dstTransactionName a cél (bevétel) oldali tranzakció neve
 * @param dstWalletId        a cél wallet
 * @param transactionDate    az átvezetés dátuma
 * @param priceSum           az átvezetett összeg (pozitív)
 */
public record TransferTransactionResponseDto(
        Long transferId,
        String srcTransactionName,
        Long srcWalletId,
        String dstTransactionName,
        Long dstWalletId,
        LocalDate transactionDate,
        BigDecimal priceSum) {}
