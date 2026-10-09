package com.starbuck.moneytracker.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import com.starbuck.moneytracker.commands.BalanceSyncCommand;
import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.entity.BalanceSync;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enum_entites.CurrencyEnum;
import com.starbuck.moneytracker.entity.enum_entites.TransactionSpecialTypeEnum;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;
import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;
import com.starbuck.moneytracker.repository.BalanceSyncRepository;
import com.starbuck.moneytracker.service.BalanceSyncService;
import com.starbuck.moneytracker.service.TransactionService;
import com.starbuck.moneytracker.service.WalletService;
import com.starbuck.moneytracker.util.TransactionFactory;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
public class BalanceSyncServiceTest {

    @Mock
    private BalanceSyncRepository balanceSyncRepo;

    @Mock
    private TransactionService transactionService;

    @Mock
    private MessageSource messageSource;

    @Mock
    private WalletService walletService;

    private BalanceSyncService balanceSyncService;

    private final User user = new User(1L, "name", "password", "email");

    private final Wallet wallet = new Wallet(1L, "wallet", user, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT);

    @BeforeEach
    void setUp() {
        var transactionFactory = new TransactionFactory(messageSource);
        balanceSyncService = new BalanceSyncService(walletService, transactionService, balanceSyncRepo,
                transactionFactory);
    }

    @Test
    void testSyncWalletWithoutCommand() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            balanceSyncService.syncWallet(null);
        });
    }

    @Test
    void testSyncWalletNotExistingWallet() {
        when(walletService.getWalletByIdForUpdate(5L)).thenThrow(new EntityNotFoundException("Wallet not found"));

        BalanceSyncCommand command = new BalanceSyncCommand(5, LocalDate.now(), new BigDecimal(50));
        assertThrowsExactly(EntityNotFoundException.class, () -> {
            balanceSyncService.syncWallet(command);
        });
    }

    @Test
    void testSyncWalletCreateOnlySyncEntry() {
        // GIVEN
        when(walletService.getWalletByIdForUpdate(1L)).thenReturn(wallet);
        when(walletService.getBalanceForWallet(1L)).thenReturn(new BigDecimal("50.00"));

        var actualDate = LocalDate.now();
        BalanceSyncCommand command = new BalanceSyncCommand(1L, actualDate, new BigDecimal("50.00"));

        ArgumentCaptor<BalanceSync> argumentCaptor = ArgumentCaptor.forClass(BalanceSync.class);

        // WHEN
        balanceSyncService.syncWallet(command);

        // THEN
        verify(balanceSyncRepo, times(1)).save(argumentCaptor.capture());
        verify(transactionService, times(0)).createTransaction(any());

        var savedSyncEntry = argumentCaptor.getValue();
        assertEquals(new BigDecimal("50.00"), savedSyncEntry.getActualBalance());
        assertEquals(actualDate, savedSyncEntry.getSyncDate());
        assertEquals("wallet", savedSyncEntry.getWallet().getName());
        assertNull(savedSyncEntry.getSyncTransaction());
    }

    @Test
    void testSyncWalletCreateSyncAndTransactionExpenseEntry() {
        // GIVEN
        var actualDate = LocalDate.now();
        when(messageSource.getMessage(eq("synchronizeTransactionNamePrefix"), any(), any(Locale.class)))
                .thenReturn("Szinkronizálás");

        when(walletService.getWalletByIdForUpdate(1L)).thenReturn(wallet);
        // 60 a balance, de a user szerint 50-et számolt össze magánál, tehát egy 10-es
        // kiadású tranzakciónak kell létrejönnie
        when(walletService.getBalanceForWallet(1L)).thenReturn(new BigDecimal("60.00"));
        var createdTransaction = new Transaction();
        when(transactionService.createTransaction(any())).thenReturn(createdTransaction);
        BalanceSyncCommand command = new BalanceSyncCommand(1, actualDate, new BigDecimal("50.00"));

        ArgumentCaptor<BalanceSync> argumentCaptorBalanceSync = ArgumentCaptor.forClass(BalanceSync.class);
        ArgumentCaptor<TransactionSaveCommand> argumentCaptorTransactionCreate = ArgumentCaptor
                .forClass(TransactionSaveCommand.class);

        // WHEN
        balanceSyncService.syncWallet(command);

        // THEN
        verify(balanceSyncRepo, times(1)).save(argumentCaptorBalanceSync.capture());
        verify(transactionService, times(1)).createTransaction(argumentCaptorTransactionCreate.capture());

        var savedSyncEntry = argumentCaptorBalanceSync.getValue();
        assertEquals(savedSyncEntry.getActualBalance(), new BigDecimal("50.00"));
        assertEquals(savedSyncEntry.getSyncDate(), actualDate);
        assertEquals(savedSyncEntry.getWallet().getName(), "wallet");
        assertSame(createdTransaction, savedSyncEntry.getSyncTransaction());

        var transactionCreateCommand = argumentCaptorTransactionCreate.getValue();
        assertEquals("Szinkronizálás: wallet", transactionCreateCommand.getTransactionName());
        assertEquals(TransactionTypeEnum.OUTCOME, transactionCreateCommand.getTransactionType());
        assertEquals(TransactionSpecialTypeEnum.SYNC, transactionCreateCommand.getSpecialType());
        assertTrue(BigDecimal.valueOf(-10.00).compareTo(transactionCreateCommand.getGlobalPrice()) == 0);
        assertEquals(actualDate, transactionCreateCommand.getTransactionDate());
        assertEquals(wallet.getId(), transactionCreateCommand.getWalletId());
    }

    @Test
    void testSyncWalletCreateSyncAndTransactionIncomeEntry() {
        // GIVEN
        var actualDate = LocalDate.now();
        when(messageSource.getMessage(eq("synchronizeTransactionNamePrefix"), any(), any(Locale.class)))
                .thenReturn("Szinkronizálás");

        when(walletService.getWalletByIdForUpdate(1L)).thenReturn(wallet);
        // 60 a balance, de a user szerint 100-at számolt össze magánál, tehát egy 40-es
        // bevételi tranzakciónak kell létrejönnie
        when(walletService.getBalanceForWallet(1L)).thenReturn(new BigDecimal("60.00"));
        var createdTransaction = new Transaction();
        when(transactionService.createTransaction(any())).thenReturn(createdTransaction);
        BalanceSyncCommand command = new BalanceSyncCommand(1, actualDate, new BigDecimal("100.00"));

        ArgumentCaptor<BalanceSync> argumentCaptorBalanceSync = ArgumentCaptor.forClass(BalanceSync.class);
        ArgumentCaptor<TransactionSaveCommand> argumentCaptorTransactionCreate = ArgumentCaptor
                .forClass(TransactionSaveCommand.class);

        // WHEN
        balanceSyncService.syncWallet(command);

        // THEN
        verify(balanceSyncRepo, times(1)).save(argumentCaptorBalanceSync.capture());
        verify(transactionService, times(1)).createTransaction(argumentCaptorTransactionCreate.capture());

        var savedSyncEntry = argumentCaptorBalanceSync.getValue();
        assertEquals(savedSyncEntry.getActualBalance(), new BigDecimal("100.00"));
        assertEquals(savedSyncEntry.getSyncDate(), actualDate);
        assertEquals(savedSyncEntry.getWallet().getName(), "wallet");
        assertSame(createdTransaction, savedSyncEntry.getSyncTransaction());

        var transactionCreateCommand = argumentCaptorTransactionCreate.getValue();
        assertEquals("Szinkronizálás: wallet", transactionCreateCommand.getTransactionName());
        assertEquals(TransactionTypeEnum.INCOME, transactionCreateCommand.getTransactionType());
        assertEquals(TransactionSpecialTypeEnum.SYNC, transactionCreateCommand.getSpecialType());
        assertTrue(BigDecimal.valueOf(40.00).compareTo(transactionCreateCommand.getGlobalPrice()) == 0);
        assertEquals(actualDate, transactionCreateCommand.getTransactionDate());
        assertEquals(wallet.getId(), transactionCreateCommand.getWalletId());
    }

    /**
     * Negatív egyenlegnél (pl. hitelkártya, folyószámlahitel) is a különbség
     * előjele dönti el a korrekciós tranzakció típusát
     */
    @ParameterizedTest(name = "rendszer: {0}, user: {1} -> {2} {3}")
    @CsvSource({
            // pozitívból negatívba
            "20.00, -30.00, OUTCOME, -50.00",
            // negatívból pozitívba
            "-20.00, 30.00, INCOME, 50.00",
            // negatívból kevésbé negatívba
            "-50.00, -20.00, INCOME, 30.00",
            // negatívból még negatívabba
            "-50.00, -80.00, OUTCOME, -30.00",
            // nullából negatívba
            "0.00, -15.50, OUTCOME, -15.50",
    })
    void testSyncWalletCreateSyncAndTransactionEntryWithNegativeBalance(String balanceInDb, String balanceFromUser,
            TransactionTypeEnum expectedType, String expectedPrice) {
        // GIVEN
        var actualDate = LocalDate.now();
        when(messageSource.getMessage(eq("synchronizeTransactionNamePrefix"), any(), any(Locale.class)))
                .thenReturn("Szinkronizálás");

        when(walletService.getWalletByIdForUpdate(1L)).thenReturn(wallet);
        when(walletService.getBalanceForWallet(1L)).thenReturn(new BigDecimal(balanceInDb));
        var createdTransaction = new Transaction();
        when(transactionService.createTransaction(any())).thenReturn(createdTransaction);
        BalanceSyncCommand command = new BalanceSyncCommand(1, actualDate, new BigDecimal(balanceFromUser));

        ArgumentCaptor<BalanceSync> argumentCaptorBalanceSync = ArgumentCaptor.forClass(BalanceSync.class);
        ArgumentCaptor<TransactionSaveCommand> argumentCaptorTransactionCreate = ArgumentCaptor
                .forClass(TransactionSaveCommand.class);

        // WHEN
        balanceSyncService.syncWallet(command);

        // THEN
        verify(balanceSyncRepo, times(1)).save(argumentCaptorBalanceSync.capture());
        verify(transactionService, times(1)).createTransaction(argumentCaptorTransactionCreate.capture());

        var savedSyncEntry = argumentCaptorBalanceSync.getValue();
        assertEquals(new BigDecimal(balanceFromUser), savedSyncEntry.getActualBalance());
        assertEquals(actualDate, savedSyncEntry.getSyncDate());
        assertSame(createdTransaction, savedSyncEntry.getSyncTransaction());

        var transactionCreateCommand = argumentCaptorTransactionCreate.getValue();
        assertEquals(expectedType, transactionCreateCommand.getTransactionType());
        assertEquals(TransactionSpecialTypeEnum.SYNC, transactionCreateCommand.getSpecialType());
        assertTrue(new BigDecimal(expectedPrice).compareTo(transactionCreateCommand.getGlobalPrice()) == 0);
        assertEquals(actualDate, transactionCreateCommand.getTransactionDate());
        assertEquals(wallet.getId(), transactionCreateCommand.getWalletId());
    }

    @Test
    void testSyncWalletCreateOnlySyncEntryWithNegativeBalance() {
        // GIVEN
        when(walletService.getWalletByIdForUpdate(1L)).thenReturn(wallet);
        when(walletService.getBalanceForWallet(1L)).thenReturn(new BigDecimal("-50.00"));

        var actualDate = LocalDate.now();
        BalanceSyncCommand command = new BalanceSyncCommand(1L, actualDate, new BigDecimal("-50.00"));

        ArgumentCaptor<BalanceSync> argumentCaptor = ArgumentCaptor.forClass(BalanceSync.class);

        // WHEN
        balanceSyncService.syncWallet(command);

        // THEN
        verify(balanceSyncRepo, times(1)).save(argumentCaptor.capture());
        verify(transactionService, times(0)).createTransaction(any());

        var savedSyncEntry = argumentCaptor.getValue();
        assertEquals(new BigDecimal("-50.00"), savedSyncEntry.getActualBalance());
        assertNull(savedSyncEntry.getSyncTransaction());
    }

}
