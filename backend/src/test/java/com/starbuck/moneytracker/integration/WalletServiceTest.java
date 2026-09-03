package com.starbuck.moneytracker.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
import com.starbuck.moneytracker.commands.UpdateWalletCommand;
import com.starbuck.moneytracker.dto.WalletListResponseDto;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enum_entites.CurrencyEnum;
import com.starbuck.moneytracker.entity.enum_entites.GeneralStatusEnum;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;
import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.repository.UserRepository;
import com.starbuck.moneytracker.repository.WalletRepository;
import com.starbuck.moneytracker.service.WalletService;
import com.starbuck.moneytracker.testsupport.MySqlContainerTest;
import com.starbuck.moneytracker.util.CurrentUserUtil;

import jakarta.persistence.EntityNotFoundException;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WalletServiceTest extends MySqlContainerTest {

    @Autowired
    WalletService walletService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TransactionRepository transactionRepo;

    @Autowired
    WalletRepository walletRepo;

    @MockitoBean
    CurrentUserUtil currentUser;

    private User user;

    @BeforeAll
    public void setUpBeforeTests() {
        var newUser = new User("walletUser", "password", "wallet@user.com");
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
     * Alapértelmezett tárcát hoz létre
     */
    @Test
    public void testDefaultWalletCreation() {
        // When
        var wallet = walletService.createDefaultWallet(this.user);

        // Then
        assertEquals("Tárca", wallet.getName());
        assertEquals(WalletTypeEnum.DEFAULT, wallet.getType());
        assertEquals(CurrencyEnum.HUF, wallet.getCurrencyCode());
        assertEquals(this.user, wallet.getUser());

        walletRepo.delete(wallet);
    }

    /**
     * Wallet, saját adatokkal
     */
    @Test
    public void testCreateCustomWallet() {
        // Given
        var command = new CreateWalletCommand("customWallet", CurrencyEnum.USD, WalletTypeEnum.SAVINGS, this.user);

        // When
        var wallet = walletService.createWallet(command);

        // Then
        assertEquals("customWallet", wallet.getName());
        assertEquals(WalletTypeEnum.SAVINGS, wallet.getType());
        assertEquals(CurrencyEnum.USD, wallet.getCurrencyCode());
        assertEquals(this.user, wallet.getUser());

        walletRepo.delete(wallet);
    }

    @Test
    public void testListWallets() {
        // Given
        // Beloginolt user tárcái
        var command1 = new CreateWalletCommand("UsdWallet", CurrencyEnum.USD, WalletTypeEnum.SAVINGS, this.user);
        var wallet1 = walletService.createWallet(command1);
        var command2 = new CreateWalletCommand("HufWallet", CurrencyEnum.HUF, WalletTypeEnum.DEFAULT, this.user);
        var wallet2 = walletService.createWallet(command2);
        var command3 = new CreateWalletCommand("Huf2Wallet", CurrencyEnum.HUF, WalletTypeEnum.DEFAULT, this.user);
        var wallet3 = walletService.createWallet(command3);

        // Másik user tárcája
        var anotherUser = new User("anotherWalletUser", "password", "another-wallet@user.com");
        anotherUser.generateUuid();
        var savedAnotherUser = userRepository.save(anotherUser);
        var wallet4 = walletService.createDefaultWallet(savedAnotherUser);

        var transaction1AfterSave = transactionRepo
                .save(new Transaction("transaction1", LocalDate.now(), TransactionTypeEnum.INCOME,
                        new BigDecimal(200), wallet2));
        var transaction2AfterSave = transactionRepo
                .save(new Transaction("transaction2", LocalDate.now(), TransactionTypeEnum.OUTCOME,
                        new BigDecimal(-50), wallet2));
        var transaction3AfterSave = transactionRepo
                .save(new Transaction("transaction3", LocalDate.now(), TransactionTypeEnum.OUTCOME,
                        new BigDecimal(-50), wallet3));
        // When
        List<WalletListResponseDto> wallets = walletService.listWalletsForUser();

        // Then
        assertEquals(3, wallets.size());

        assertEquals("UsdWallet", wallets.get(0).name());
        assertEquals(CurrencyEnum.USD, wallets.get(0).currencyCode());
        assertEquals(WalletTypeEnum.SAVINGS, wallets.get(0).type());
        assertEquals(new BigDecimal("0.00"), wallets.get(0).sum());

        assertEquals("HufWallet", wallets.get(1).name());
        assertEquals(CurrencyEnum.HUF, wallets.get(1).currencyCode());
        assertEquals(WalletTypeEnum.DEFAULT, wallets.get(1).type());
        assertEquals(new BigDecimal("150.00"), wallets.get(1).sum());

        assertEquals("Huf2Wallet", wallets.get(2).name());
        assertEquals(CurrencyEnum.HUF, wallets.get(2).currencyCode());
        assertEquals(WalletTypeEnum.DEFAULT, wallets.get(2).type());
        assertEquals(new BigDecimal("-50.00"), wallets.get(2).sum());

        transactionRepo.hardDeleteTransaction(transaction1AfterSave.getId());
        transactionRepo.hardDeleteTransaction(transaction2AfterSave.getId());
        transactionRepo.hardDeleteTransaction(transaction3AfterSave.getId());
        walletRepo.delete(wallet1);
        walletRepo.delete(wallet2);
        walletRepo.delete(wallet3);
        walletRepo.delete(wallet4);
        userRepository.delete(savedAnotherUser);
    }

    @Test
    public void updateWallet() {
        // Given
        var createCommand = new CreateWalletCommand("customWallet", CurrencyEnum.USD, WalletTypeEnum.SAVINGS,
                this.user);
        var wallet = walletService.createWallet(createCommand);

        var updateCommand = new UpdateWalletCommand("UpdatedCustomWallet", WalletTypeEnum.DEFAULT);

        // When
        walletService.updateWallet(wallet.getId(), updateCommand);

        // Then
        Wallet updatedWallet = walletRepo.findById(wallet.getId())
                .orElseThrow(() -> new EntityNotFoundException("no wallet found"));

        assertEquals("UpdatedCustomWallet", updatedWallet.getName());
        assertEquals(WalletTypeEnum.DEFAULT, updatedWallet.getType());
        // Ezek nem módosultak
        assertEquals(createCommand.getCurrency(), updatedWallet.getCurrencyCode());
        assertEquals(createCommand.getUser().getId(), updatedWallet.getUser().getId());

        walletRepo.delete(updatedWallet);
    }

    @Test
    public void testGetWalletById() {
        // Given
        var command = new CreateWalletCommand("customWallet", CurrencyEnum.USD, WalletTypeEnum.SAVINGS, this.user);
        var wallet = walletService.createWallet(command);

        // When
        var foundWallet = walletService.getWalletById(wallet.getId());

        // Then
        assertEquals(wallet.getId(), foundWallet.getId());
        assertEquals("customWallet", foundWallet.getName());
        assertEquals(this.user.getId(), foundWallet.getUser().getId());

        walletRepo.delete(wallet);
    }

    @Test
    public void testSoftDeleteWallet() {
        // Given
        var command = new CreateWalletCommand("customWallet", CurrencyEnum.USD, WalletTypeEnum.SAVINGS, this.user);
        var wallet = walletService.createWallet(command);

        // When
        walletService.softDeleteWallet(wallet.getId());

        // Then
        var deletedWallet = walletRepo.findById(wallet.getId())
                .orElseThrow(() -> new EntityNotFoundException("no wallet found"));
        assertEquals(GeneralStatusEnum.DISABLED, deletedWallet.getStatus());

        assertThrows(EntityNotFoundException.class, () -> {
            walletService.getWalletById(wallet.getId());
        });

        walletRepo.delete(deletedWallet);
    }

    // HIBÁS ESETEK

    /**
     * Hibát dob, ha nincs beállítva user
     */
    @Test
    public void testUserNullAtDefaultWalletCreation_throwException() {
        assertThrows(IllegalArgumentException.class, () -> {
            walletService.createDefaultWallet(null);
        });
    }

    /**
     * Hibát dob, ha nincs beállítva a parancs
     */
    @Test
    public void testCommandNullAtCreateWallet_throwException() {
        assertThrows(IllegalArgumentException.class, () -> {
            walletService.createWallet(null);
        });
    }

    /**
     * Hibát dob, ha nincs beállítva a parancs
     */
    @Test
    public void testCommandNullAtUpdateWallet_throwException() {
        assertThrows(IllegalArgumentException.class, () -> {
            walletService.updateWallet(user.getId(), null);
        });
    }

    @Test
    public void testWalletNotExist_throwException() {
        // Given
        var anotherUser = new User("anotherWalletUser", "password", "another-wallet@user.com");
        anotherUser.generateUuid();
        var savedAnotherUser = userRepository.save(anotherUser);
        var anotherUserWallet = walletService.createDefaultWallet(savedAnotherUser);

        var updateCommand = new UpdateWalletCommand("UpdatedCustomWallet", WalletTypeEnum.DEFAULT);

        // When
        assertThrows(EntityNotFoundException.class, () -> {
            walletService.updateWallet(anotherUserWallet.getId(), updateCommand);
        });

        // Then
        walletRepo.delete(anotherUserWallet);
        userRepository.delete(savedAnotherUser);
    }

    @Test
    public void testGetWalletById_anotherUsersWallet_throwException() {
        // Given
        var anotherUser = new User("anotherWalletUser", "password", "another-wallet@user.com");
        anotherUser.generateUuid();
        var savedAnotherUser = userRepository.save(anotherUser);
        var anotherUserWallet = walletService.createDefaultWallet(savedAnotherUser);

        // When
        assertThrows(EntityNotFoundException.class, () -> {
            walletService.getWalletById(anotherUserWallet.getId());
        });

        // Then
        walletRepo.delete(anotherUserWallet);
        userRepository.delete(savedAnotherUser);
    }

    @Test
    public void testSoftDeleteWallet_anotherUsersWallet_throwException() {
        // Given
        var anotherUser = new User("anotherWalletUser", "password", "another-wallet@user.com");
        anotherUser.generateUuid();
        var savedAnotherUser = userRepository.save(anotherUser);
        var anotherUserWallet = walletService.createDefaultWallet(savedAnotherUser);

        // When
        assertThrows(EntityNotFoundException.class, () -> {
            walletService.softDeleteWallet(anotherUserWallet.getId());
        });

        // Then
        walletRepo.delete(anotherUserWallet);
        userRepository.delete(savedAnotherUser);
    }
}
