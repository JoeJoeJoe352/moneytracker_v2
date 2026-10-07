package com.starbuck.moneytracker.util;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.commands.SyncWalletCommand;
import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enum_entites.TransactionSpecialTypeEnum;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;

@Component
public class TransactionFactory {
    private final MessageSource messageSource;

    public TransactionFactory(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Létrehoz egy tranzakciót a szinkronizált adatok alapján
     * 
     * @param command
     * @param wallet
     * @param balanceOfWalletInDb
     */
    public TransactionSaveCommand createTransactionEntryFromBalanceChange(SyncWalletCommand command, Wallet wallet,
            BigDecimal balanceOfWalletInDb) {

        if (wallet == null || balanceOfWalletInDb == null) {
            throw new IllegalArgumentException("wallet or balanceOfWalletInDb is null");
        }
        String transactionNamePrefixLocalised = messageSource.getMessage("synchronizeTransactionNamePrefix", null,
                LocaleContextHolder.getLocale());
        String transactionName = transactionNamePrefixLocalised + ": " + wallet.getName();

        var differenceFromActualBalance = command.getBalanceFromUser().subtract(balanceOfWalletInDb);
        int differenceFromZero = differenceFromActualBalance.compareTo(BigDecimal.ZERO);

        if (differenceFromZero == 0) {
            throw new IllegalArgumentException("Cannot create transaction with 0 price.");
        }

        TransactionTypeEnum type = differenceFromZero > 0 ? TransactionTypeEnum.INCOME : TransactionTypeEnum.OUTCOME;

        return new TransactionSaveCommand(transactionName, differenceFromActualBalance,
                command.getSyncDate(), type, List.of(), List.of(), wallet.getId())
                .withSpecialType(TransactionSpecialTypeEnum.SYNC);

    }
}
