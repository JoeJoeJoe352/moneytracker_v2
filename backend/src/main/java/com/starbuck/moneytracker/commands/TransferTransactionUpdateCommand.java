package com.starbuck.moneytracker.commands;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Meglévő átvezetés két oldalának együttes módosítása
 */
public class TransferTransactionUpdateCommand extends AbstractTransferTransactionSaveCommand {

    private Long srcTransactionId;
    private Long dstTransactionId;

    /**
     * @param srcTransactionName A forrás (kiadás) oldali tranzakció új neve
     * @param srcTransactionId   A forrás oldali tranzakció
     * @param dstTransactionName A cél (bevétel) oldali tranzakció új neve
     * @param dstTransactionId   A cél oldali tranzakció (nem lehet azonos a
     *                           forrással)
     * @param transactionDate    Az átvezetés új dátuma
     * @param priceSum           Az átvezetett új összeg (pozitív)
     */
    public TransferTransactionUpdateCommand(
            String srcTransactionName,
            Long srcTransactionId,
            String dstTransactionName,
            Long dstTransactionId,
            LocalDate transactionDate,
            BigDecimal priceSum) {
        super(srcTransactionName, dstTransactionName, transactionDate, priceSum);

        if (srcTransactionId == null) {
            throw new IllegalArgumentException("Source transactionId cannot be null");
        }
        if (dstTransactionId == null) {
            throw new IllegalArgumentException("Destination transactionId cannot be null");
        }
        if (srcTransactionId.equals(dstTransactionId)) {
            throw new IllegalArgumentException("Source and destination transaction must be different");
        }

        this.srcTransactionId = srcTransactionId;
        this.dstTransactionId = dstTransactionId;
    }

    public Long getSrcTransactionId() {
        return srcTransactionId;
    }

    public Long getDstTransactionId() {
        return dstTransactionId;
    }
}
