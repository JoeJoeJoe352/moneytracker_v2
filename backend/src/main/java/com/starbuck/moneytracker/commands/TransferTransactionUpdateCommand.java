package com.starbuck.moneytracker.commands;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Meglévő átvezetés két oldalának együttes módosítása
 */
public class TransferTransactionUpdateCommand extends AbstractTransferTransactionSaveCommand {

    /**
     * @param srcTransactionName A forrás (kiadás) oldali tranzakció új neve
     * @param dstTransactionName A cél (bevétel) oldali tranzakció új neve
     * @param transactionDate    Az átvezetés új dátuma
     * @param priceSum           Az átvezetett új összeg (pozitív)
     */
    public TransferTransactionUpdateCommand(
            String srcTransactionName, String dstTransactionName, LocalDate transactionDate, BigDecimal priceSum) {
        super(srcTransactionName, dstTransactionName, transactionDate, priceSum);
    }
}
