package com.starbuck.moneytracker.commands;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;

public class TransactionCreateCommand extends TransactionSaveCommand {

    /**
     * 
     * @param name              Tranzakció neve
     * @param globalPrice       Tranzakció ára (lehet null, ha detailban lesz)
     * @param date              Tranzakció ideje
     * @param type              Tranzakció típusa
     * @param detailCommands    Tranzakcióhoz tartozó detail-ek (lehet üres)
     * @param categories        Tranzakcióhoz kategóriái (lehet üres)
     */
    public TransactionCreateCommand(String name, BigDecimal globalPrice, LocalDate date, TransactionTypeEnum type,
            List<TransactionDetailSaveCommand> detailCommands, List<Long> categories, Long walletId) {
        super(name, globalPrice, date, type, detailCommands, categories, walletId);
    }

}
