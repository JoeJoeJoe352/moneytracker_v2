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

import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enum_entites.CurrencyEnum;
import com.starbuck.moneytracker.entity.enum_entites.TransactionSpecialTypeEnum;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;
import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;
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

        var result = transactionFactory.createTransactionEntryFromBalanceChange(wallet, new BigDecimal("50.00"),
                new BigDecimal("60.00"), date);

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
        var result = transactionFactory.createTransactionEntryFromBalanceChange(wallet, new BigDecimal("100.00"),
                new BigDecimal("60.00"), LocalDate.of(2026, 10, 1));

        assertEquals(TransactionTypeEnum.INCOME, result.getTransactionType());
        assertEquals(new BigDecimal("40.00"), result.getGlobalPrice());
    }

    @Test
    void testCreateWithEqualBalancesThrows() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            transactionFactory.createTransactionEntryFromBalanceChange(wallet, new BigDecimal("60.00"),
                    new BigDecimal("60.00"), LocalDate.of(2026, 10, 1));
        });
    }

    @Test
    void testCreateWithMissingBalanceFromUserThrows() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            transactionFactory.createTransactionEntryFromBalanceChange(wallet, null, new BigDecimal("60.00"),
                    LocalDate.of(2026, 10, 1));
        });
    }

    @Test
    void testCreateWithMissingDateThrows() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            transactionFactory.createTransactionEntryFromBalanceChange(wallet, new BigDecimal("50.00"),
                    new BigDecimal("60.00"), null);
        });
    }
}
