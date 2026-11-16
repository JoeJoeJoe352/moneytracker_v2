package com.starbuck.moneytracker.commands;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Átvezetés létrehozása: a forrás walletből a cél walletbe
 */
public class TransferTransactionCreateCommand extends AbstractTransferTransactionSaveCommand {

    private Long srcWalletId;
    private Long dstWalletId;

    /**
     * @param srcTransactionName A forrás (kiadás) oldali tranzakció neve
     * @param srcWalletId        A forrás wallet
     * @param dstTransactionName A cél (bevétel) oldali tranzakció neve
     * @param dstWalletId        A cél wallet (nem lehet azonos a forrással)
     * @param transactionDate    Az átvezetés dátuma
     * @param priceSum           Az átvezetett összeg (pozitív)
     */
    public TransferTransactionCreateCommand(
            String srcTransactionName,
            Long srcWalletId,
            String dstTransactionName,
            Long dstWalletId,
            LocalDate transactionDate,
            BigDecimal priceSum) {
        super(srcTransactionName, dstTransactionName, transactionDate, priceSum);

        if (srcWalletId == null) {
            throw new IllegalArgumentException("Source walletId cannot be null");
        }
        if (dstWalletId == null) {
            throw new IllegalArgumentException("Destination walletId cannot be null");
        }
        if (srcWalletId.equals(dstWalletId)) {
            throw new IllegalArgumentException("Source and destination wallet must be different");
        }

        this.srcWalletId = srcWalletId;
        this.dstWalletId = dstWalletId;
    }

    public Long getSrcWalletId() {
        return srcWalletId;
    }

    public Long getDstWalletId() {
        return dstWalletId;
    }
}
