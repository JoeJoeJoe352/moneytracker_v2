package com.starbuck.moneytracker.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.starbuck.moneytracker.commands.CreateWalletCommand;
import com.starbuck.moneytracker.commands.SyncWalletCommand;
import com.starbuck.moneytracker.entity.BalanceSync;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enum_entites.CurrencyEnum;
import com.starbuck.moneytracker.entity.enum_entites.TransactionSpecialTypeEnum;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;
import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;
import com.starbuck.moneytracker.repository.BalanceSyncRepository;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.repository.UserRepository;
import com.starbuck.moneytracker.repository.WalletRepository;
import com.starbuck.moneytracker.service.BalanceSyncService;
import com.starbuck.moneytracker.service.WalletService;
import com.starbuck.moneytracker.testsupport.MySqlContainerTest;
import com.starbuck.moneytracker.util.CurrentUserUtil;

import jakarta.persistence.EntityNotFoundException;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BalanceSyncServiceIntegrationTest extends MySqlContainerTest {

    @Autowired
    BalanceSyncService balanceSyncService;

    @Autowired
    WalletService walletService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    WalletRepository walletRepo;

    @Autowired
    TransactionRepository transactionRepo;

    @Autowired
    TransactionDetailRepository transactionDetailRepo;

    @Autowired
    BalanceSyncRepository balanceSyncRepo;

    @MockitoBean
    CurrentUserUtil currentUser;

    private User user;

    @BeforeAll
    public void setUpBeforeTests() {
        var newUser = new User("balanceSyncUser", "password", "balance-sync@user.com");
        newUser.generateUuid();
        this.user = userRepository.save(newUser);

        LocaleContextHolder.setLocale(Locale.forLanguageTag("hu"));
    }

    @AfterAll
    public void resetAfterTests() {
        this.userRepository.delete(this.user);

        LocaleContextHolder.resetLocaleContext();
    }

    @BeforeEach
    public void setActualUser() {
        Mockito.when(currentUser.getUser()).thenReturn(this.user);
    }

    /**
     * Egyező egyenlegnél csak a sync bejegyzés jön létre, korrekciós tranzakció
     * nem
     */
    @Test
    public void testSyncWalletWithEqualBalance() {
        // Given
        var wallet = createWallet("syncWallet");
        var initialTransaction = saveIncome(wallet, new BigDecimal("60.00"));
        var syncDate = LocalDate.of(2026, 10, 1);

        // When
        balanceSyncService.syncWallet(new SyncWalletCommand(wallet.getId(), syncDate, new BigDecimal("60.00")));

        // Then
        var balanceSync = findSyncForWallet(wallet);
        assertEquals(new BigDecimal("60.00"), balanceSync.getActualBalance());
        assertEquals(syncDate, balanceSync.getSyncDate());
        assertEquals(wallet.getId(), balanceSync.getWallet().getId());
        assertNull(balanceSync.getSyncTransaction());
        assertEquals(new BigDecimal("60.00"), walletService.getBalanceForWallet(wallet.getId()));

        balanceSyncRepo.delete(balanceSync);
        transactionRepo.hardDeleteTransaction(initialTransaction.getId());
        walletRepo.delete(wallet);
    }

    /**
     * A user kevesebb pénzt számolt, mint ami a walletben van: kiadás jön létre a
     * különbözettel
     */
    @Test
    public void testSyncWalletWithLowerBalanceCreatesOutcome() {
        // Given
        var wallet = createWallet("syncWallet");
        var initialTransaction = saveIncome(wallet, new BigDecimal("60.00"));
        var syncDate = LocalDate.of(2026, 10, 1);

        // When
        balanceSyncService.syncWallet(new SyncWalletCommand(wallet.getId(), syncDate, new BigDecimal("50.00")));

        // Then
        var balanceSync = findSyncForWallet(wallet);
        assertEquals(new BigDecimal("50.00"), balanceSync.getActualBalance());
        assertEquals(syncDate, balanceSync.getSyncDate());

        Transaction syncTransaction = balanceSync.getSyncTransaction();
        assertNotNull(syncTransaction);
        assertEquals("Szinkronizálás: syncWallet", syncTransaction.getName());
        assertEquals(TransactionTypeEnum.OUTCOME, syncTransaction.getTransactionType());
        assertEquals(TransactionSpecialTypeEnum.SYNC, syncTransaction.getSpecialType());
        assertEquals(new BigDecimal("-10.00"), syncTransaction.getPriceSum());
        assertEquals(syncDate, syncTransaction.getTransactionDate());
        assertEquals(wallet.getId(), syncTransaction.getWallet().getId());

        // A korrekció után a wallet egyenlege megegyezik a user által megadottal
        assertEquals(new BigDecimal("50.00"), walletService.getBalanceForWallet(wallet.getId()));

        balanceSyncRepo.delete(balanceSync);
        deleteSyncTransaction(syncTransaction);
        transactionRepo.hardDeleteTransaction(initialTransaction.getId());
        walletRepo.delete(wallet);
    }

    /**
     * A user több pénzt számolt, mint ami a walletben van: bevétel jön létre a
     * különbözettel
     */
    @Test
    public void testSyncWalletWithHigherBalanceCreatesIncome() {
        // Given
        var wallet = createWallet("syncWallet");
        var initialTransaction = saveIncome(wallet, new BigDecimal("60.00"));
        var syncDate = LocalDate.of(2026, 10, 1);

        // When
        balanceSyncService.syncWallet(new SyncWalletCommand(wallet.getId(), syncDate, new BigDecimal("100.00")));

        // Then
        var balanceSync = findSyncForWallet(wallet);
        assertEquals(new BigDecimal("100.00"), balanceSync.getActualBalance());

        Transaction syncTransaction = balanceSync.getSyncTransaction();
        assertNotNull(syncTransaction);
        assertEquals(TransactionTypeEnum.INCOME, syncTransaction.getTransactionType());
        assertEquals(TransactionSpecialTypeEnum.SYNC, syncTransaction.getSpecialType());
        assertEquals(new BigDecimal("40.00"), syncTransaction.getPriceSum());

        assertEquals(new BigDecimal("100.00"), walletService.getBalanceForWallet(wallet.getId()));

        balanceSyncRepo.delete(balanceSync);
        deleteSyncTransaction(syncTransaction);
        transactionRepo.hardDeleteTransaction(initialTransaction.getId());
        walletRepo.delete(wallet);
    }

    /**
     * Üres walletnél a teljes megadott összeg bevételként jelenik meg
     */
    @Test
    public void testSyncEmptyWalletCreatesIncome() {
        // Given
        var wallet = createWallet("emptySyncWallet");
        var syncDate = LocalDate.of(2026, 10, 1);

        // When
        balanceSyncService.syncWallet(new SyncWalletCommand(wallet.getId(), syncDate, new BigDecimal("25.50")));

        // Then
        var balanceSync = findSyncForWallet(wallet);
        Transaction syncTransaction = balanceSync.getSyncTransaction();
        assertNotNull(syncTransaction);
        assertEquals(TransactionTypeEnum.INCOME, syncTransaction.getTransactionType());
        assertEquals(new BigDecimal("25.50"), syncTransaction.getPriceSum());
        assertEquals(new BigDecimal("25.50"), walletService.getBalanceForWallet(wallet.getId()));

        balanceSyncRepo.delete(balanceSync);
        deleteSyncTransaction(syncTransaction);
        walletRepo.delete(wallet);
    }

    // HIBÁS ESETEK

    /**
     * Más user walletjét nem lehet szinkronizálni, és semmi sem kerül mentésre
     */
    @Test
    public void testSyncAnotherUsersWallet_throwException() {
        // Given
        var anotherUser = new User("anotherBalanceSyncUser", "password", "another-balance-sync@user.com");
        anotherUser.generateUuid();
        var savedAnotherUser = userRepository.save(anotherUser);
        var anotherUserWallet = walletService.createDefaultWallet(savedAnotherUser);
        var syncCountBefore = balanceSyncRepo.count();

        // When
        assertThrows(EntityNotFoundException.class, () -> {
            balanceSyncService.syncWallet(
                    new SyncWalletCommand(anotherUserWallet.getId(), LocalDate.now(), new BigDecimal("50.00")));
        });

        // Then
        assertEquals(syncCountBefore, balanceSyncRepo.count());

        walletRepo.delete(anotherUserWallet);
        userRepository.delete(savedAnotherUser);
    }

    @Test
    public void testSyncWithoutCommand_throwException() {
        assertThrows(IllegalArgumentException.class, () -> {
            balanceSyncService.syncWallet(null);
        });
    }

    private Wallet createWallet(String name) {
        return walletService.createWallet(
                new CreateWalletCommand(name, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT, this.user));
    }

    private Transaction saveIncome(Wallet wallet, BigDecimal amount) {
        return transactionRepo.save(new Transaction("income", LocalDate.of(2026, 9, 1), TransactionTypeEnum.INCOME,
                amount, wallet));
    }

    /**
     * A TransactionService a korrekciós tranzakcióhoz detailt is ment, ezt a
     * tranzakció előtt törölni kell
     */
    private void deleteSyncTransaction(Transaction syncTransaction) {
        var details = transactionDetailRepo.findAll().stream()
                .filter(detail -> detail.getTransaction().getId().equals(syncTransaction.getId()))
                .toList();
        transactionDetailRepo.deleteAll(details);
        transactionRepo.hardDeleteTransaction(syncTransaction.getId());
    }

    private BalanceSync findSyncForWallet(Wallet wallet) {
        var syncs = balanceSyncRepo.findAll().stream()
                .filter(sync -> sync.getWallet().getId().equals(wallet.getId()))
                .toList();
        assertEquals(1, syncs.size());
        return syncs.get(0);
    }
}
