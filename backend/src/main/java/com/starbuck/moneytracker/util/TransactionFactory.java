package com.starbuck.moneytracker.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

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
     * Létrehoz egy korrekciós tranzakciót a user által megadott és a db-ben lévő egyenleg különbségéből
     *
     * @param wallet              a szinkronizált wallet
     * @param balanceFromUser     a user által megadott, valós egyenleg
     * @param balanceOfWalletInDb a wallet db-ben nyilvántartott egyenlege
     * @param date                a tranzakció dátuma (a szinkronizálás napja)
     */
    public TransactionSaveCommand createTransactionEntryFromBalanceChange(Wallet wallet, BigDecimal balanceFromUser,
            BigDecimal balanceOfWalletInDb, LocalDate date) {

        if (wallet == null || balanceFromUser == null || balanceOfWalletInDb == null || date == null) {
            throw new IllegalArgumentException("wallet, balanceFromUser, balanceOfWalletInDb or date is null");
        }
        String transactionNamePrefixLocalised = messageSource.getMessage("synchronizeTransactionNamePrefix", null,
                LocaleContextHolder.getLocale());
        String transactionName = transactionNamePrefixLocalised + ": " + wallet.getName();

        var differenceFromActualBalance = balanceFromUser.subtract(balanceOfWalletInDb);
        int differenceFromZero = differenceFromActualBalance.compareTo(BigDecimal.ZERO);

        if (differenceFromZero == 0) {
            throw new IllegalArgumentException("Cannot create transaction with 0 price.");
        }

        TransactionTypeEnum type = differenceFromZero > 0 ? TransactionTypeEnum.INCOME : TransactionTypeEnum.OUTCOME;

        return new TransactionSaveCommand(transactionName, differenceFromActualBalance,
                date, type, List.of(), List.of(), wallet.getId())
                .withSpecialType(TransactionSpecialTypeEnum.SYNC);
    }
}
