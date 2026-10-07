package com.starbuck.moneytracker.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import com.starbuck.moneytracker.commands.SyncWalletCommand;
import com.starbuck.moneytracker.entity.BalanceSync;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enum_entites.CurrencyEnum;
import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;
import com.starbuck.moneytracker.repository.BalanceSyncRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.repository.WalletRepository;
import com.starbuck.moneytracker.service.WalletService;
import com.starbuck.moneytracker.util.CurrentUserUtil;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private WalletRepository walletRepo;

    @Mock
    private BalanceSyncRepository syncRepo;

    @Mock
    private MessageSource messageSource;

    @Mock
    private CurrentUserUtil currentUser;

    @Mock
    private TransactionRepository transactionRepo;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        var user = new User(1L, "name", "password", "email");
        walletService = new WalletService(walletRepo, messageSource, currentUser, syncRepo, transactionRepo);
        Mockito.lenient().when(currentUser.getUser()).thenReturn(user);
        Mockito.lenient().when(walletRepo.getWalletById(1, 1))
                .thenReturn(Optional.of(new Wallet("wallet", user, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT)));
    }

    @Test
    void testSyncWalletWithoutCommand() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            walletService.syncWallet(null);
        });
    }

    @Test
    void testSyncWalletNotExistingWallet() {
        SyncWalletCommand command = new SyncWalletCommand(5, LocalDate.now(), new BigDecimal(50));
        assertThrowsExactly(EntityNotFoundException.class, () -> {
            walletService.syncWallet(command);
        });
    }

    @Test
    void testSyncWalletCreateOnlySyncEntry() {
        // GIVEN
        var actualDate = LocalDate.now();
        SyncWalletCommand command = new SyncWalletCommand(1, LocalDate.now(), new BigDecimal("50.00"));

        ArgumentCaptor<BalanceSync> argumentCaptor = ArgumentCaptor.forClass(BalanceSync.class);

        // WHEN
        walletService.syncWallet(command);

        // THEN
        verify(syncRepo, times(1)).save(argumentCaptor.capture());

        var savedSyncEntry = argumentCaptor.getValue();
        assertEquals(new BigDecimal("50.00"), savedSyncEntry.getActualBalance());
        assertEquals(actualDate, savedSyncEntry.getSyncDate());
        assertEquals("wallet", savedSyncEntry.getWallet().getName());
        assertNull(savedSyncEntry.getSyncTransaction());
    }

    @Test
    void testSyncWalletCreateSyncAndTransactionEntry() {
        // GIVEN
        var actualDate = LocalDate.now();
        SyncWalletCommand command = new SyncWalletCommand(1, LocalDate.now(), new BigDecimal("50.00"));

        ArgumentCaptor<BalanceSync> argumentCaptorBalanceSync = ArgumentCaptor.forClass(BalanceSync.class);
        ArgumentCaptor<Transaction> argumentCaptorTransaction = ArgumentCaptor.forClass(Transaction.class);

        // WHEN
        walletService.syncWallet(command);

        // THEN
        verify(syncRepo, times(1)).save(argumentCaptorBalanceSync.capture());
        // verify(transactionRepo, times(1)).save(argumentCaptorTransaction.capture());

        var savedSyncEntry = argumentCaptorBalanceSync.getValue();
        assertEquals(savedSyncEntry.getActualBalance(), new BigDecimal("50.00"));
        assertEquals(savedSyncEntry.getSyncDate(), actualDate);
        assertEquals(savedSyncEntry.getWallet().getName(), "wallet");
        assertNull(savedSyncEntry.getSyncTransaction());

    }

}
