package com.starbuck.moneytracker.commands;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;

public class TransactionUpdateCommand extends TransactionSaveCommand {

    /**
     * 
     * @param name              Tranzakció neve
     * @param globalPrice       Tranzakció ára (lehet null, ha detailban lesz)
     * @param date              Tranzakció ideje
     * @param type              Tranzakció típusa
     * @param detailCommands    Tranzakcióhoz tartozó detail-ek
     * @param categories        Tranzakcióhoz kategóriái (lehet üres)
     */
    public TransactionUpdateCommand(String name, BigDecimal globalPrice, LocalDate date, TransactionTypeEnum type,
            List<TransactionDetailSaveCommand> detailCommands, List<Long> categories) {
        if (detailCommands == null || detailCommands.size() == 0) {
            throw new IllegalArgumentException("Transaction detail list cannot be null at update");
        }
        super(name, globalPrice, date, type, detailCommands, categories);
    }

}
