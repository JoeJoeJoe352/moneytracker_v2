package com.starbuck.moneytracker.unit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.RecurringTransaction;
import com.starbuck.moneytracker.entity.RecurringTransactionCategory;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enums.CurrencyEnum;
import com.starbuck.moneytracker.entity.enums.LangEnum;
import com.starbuck.moneytracker.entity.enums.RecurrenceIntervalEnum;
import com.starbuck.moneytracker.entity.enums.TransactionSpecialTypeEnum;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;
import com.starbuck.moneytracker.entity.enums.WalletTypeEnum;
import com.starbuck.moneytracker.util.TransactionFactory;

@ExtendWith(MockitoExtension.class)
public class TransactionFactoryTest {

    @Mock
    private MessageSource messageSource;

    private TransactionFactory transactionFactory;

    private final User user = new User(1L, "name", "password", "email");

    private final Wallet wallet = new Wallet(1L, "wallet", user, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT);

    @BeforeEach
    void setUp() {
        transactionFactory = new TransactionFactory(messageSource);
    }

    @Test
    void testCreateOutcomeWhenUserBalanceIsLower() {
        when(messageSource.getMessage(eq("synchronizeTransactionNamePrefix"), any(), any(Locale.class)))
                .thenReturn("Szinkronizálás");
        var date = LocalDate.of(2026, 10, 1);

        var result = transactionFactory.createTransactionCommandFromBalanceChange(
                wallet, new BigDecimal("50.00"), new BigDecimal("60.00"), date);

        assertEquals("Szinkronizálás: wallet", result.getTransactionName());
        assertEquals(TransactionTypeEnum.OUTCOME, result.getTransactionType());
        assertEquals(TransactionSpecialTypeEnum.SYNC, result.getSpecialType());
        assertEquals(new BigDecimal("-10.00"), result.getGlobalPrice());
        assertEquals(date, result.getTransactionDate());
        assertEquals(1L, result.getWalletId());
        assertEquals(List.of(), result.getDetailCommands());
        assertEquals(List.of(), result.getCategories());
    }

    @Test
    void testCreateIncomeWhenUserBalanceIsHigher() {
        when(messageSource.getMessage(eq("synchronizeTransactionNamePrefix"), any(), any(Locale.class)))
                .thenReturn("Szinkronizálás");
        var result = transactionFactory.createTransactionCommandFromBalanceChange(
                wallet, new BigDecimal("100.00"), new BigDecimal("60.00"), LocalDate.of(2026, 10, 1));

        assertEquals(TransactionTypeEnum.INCOME, result.getTransactionType());
        assertEquals(new BigDecimal("40.00"), result.getGlobalPrice());
    }

    @Test
    void testCreateWithEqualBalancesThrows() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            transactionFactory.createTransactionCommandFromBalanceChange(
                    wallet, new BigDecimal("60.00"), new BigDecimal("60.00"), LocalDate.of(2026, 10, 1));
        });
    }

    @Test
    void testCreateWithMissingBalanceFromUserThrows() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            transactionFactory.createTransactionCommandFromBalanceChange(
                    wallet, null, new BigDecimal("60.00"), LocalDate.of(2026, 10, 1));
        });
    }

    @Test
    void testCreateWithMissingDateThrows() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            transactionFactory.createTransactionCommandFromBalanceChange(
                    wallet, new BigDecimal("50.00"), new BigDecimal("60.00"), null);
        });
    }

    @Test
    void testCreateFromRecurringTransactionCopiesTemplateData() {
        var recurringTransaction = recurringTransaction(TransactionTypeEnum.OUTCOME, new BigDecimal("-10.00"));
        var category1 = new Category(3L, "előfizetés", user, LangEnum.HU);
        var category2 = new Category(4L, "szórakozás", user, LangEnum.HU);
        recurringTransaction.setCategoryLinks(List.of(
                new RecurringTransactionCategory(recurringTransaction, category1),
                new RecurringTransactionCategory(recurringTransaction, category2)));
        // a tranzakció dátuma a kapott dátum, nem a recurring kezdő dátuma
        var date = LocalDate.of(2026, 11, 30);

        var result =
                transactionFactory.createTransactionCommandFromRecurrentTransactionTemplate(recurringTransaction, date);

        assertEquals("Spotify", result.getTransactionName());
        assertEquals(new BigDecimal("-10.00"), result.getGlobalPrice());
        assertEquals(date, result.getTransactionDate());
        assertEquals(TransactionTypeEnum.OUTCOME, result.getTransactionType());
        assertEquals(TransactionSpecialTypeEnum.RECURRING, result.getSpecialType());
        assertEquals(1L, result.getWalletId());
        assertEquals(List.of(), result.getDetailCommands());
        assertEquals(List.of(3L, 4L), result.getCategories());
    }

    @Test
    void testCreateFromRecurringTransactionWithoutCategories() {
        // nem állítunk be kategória kapcsolatot, az alapértelmezett üres lista marad
        var recurringTransaction = recurringTransaction(TransactionTypeEnum.OUTCOME, new BigDecimal("-10.00"));

        var result = transactionFactory.createTransactionCommandFromRecurrentTransactionTemplate(
                recurringTransaction, LocalDate.of(2026, 11, 30));

        assertEquals(List.of(), result.getCategories());
    }

    @Test
    void testCreateFromRecurringIncomeTransactionKeepsType() {
        var recurringTransaction = recurringTransaction(TransactionTypeEnum.INCOME, new BigDecimal("500.00"));

        var result = transactionFactory.createTransactionCommandFromRecurrentTransactionTemplate(
                recurringTransaction, LocalDate.of(2026, 11, 30));

        assertEquals(TransactionTypeEnum.INCOME, result.getTransactionType());
        assertEquals(new BigDecimal("500.00"), result.getGlobalPrice());
    }

    private RecurringTransaction recurringTransaction(TransactionTypeEnum type, BigDecimal priceSum) {
        var recurringTransaction = new RecurringTransaction(
                "Spotify", priceSum, LocalDate.of(2026, 10, 31), RecurrenceIntervalEnum.MONTHLY, type, wallet);
        recurringTransaction.setId(5L);
        return recurringTransaction;
    }
}
