package com.starbuck.moneytracker.util;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.commands.TransactionDetailSaveCommand;
import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;

@Component
public class TransactionDetailFactory {

    /**
     * Visszaadja a mentendő detailokat: a megadottakat, vagy ha nincs egy sem,
     * egy alapértelmezett detailt.
     *
     * @param createCommand
     * @param savedTransaction
     * @return
     */
    public List<TransactionDetailSaveCommand> resolveDetailCommands(TransactionSaveCommand createCommand,
            Transaction savedTransaction) {
        if (createCommand.getDetailCommands().isEmpty()) {
            return List.of(createDefaultDetailSaveCommand(
                    savedTransaction.getPriceSum(), createCommand.getCategories(),
                    savedTransaction.getTransactionType()));
        }
        return createCommand.getDetailCommands();
    }

    /**
     * Létrehoz egy alapértelmezett detailt
     *
     * @param price
     * @return
     */
    private TransactionDetailSaveCommand createDefaultDetailSaveCommand(BigDecimal price, List<Long> categories,
            TransactionTypeEnum type) {
        return new TransactionDetailSaveCommand(TransactionDetail.DEFAULT_DETAIL_NAME, price, categories, type);
    }
}
