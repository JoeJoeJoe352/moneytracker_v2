package com.starbuck.moneytracker.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enum_entites.CurrencyEnum;
import com.starbuck.moneytracker.entity.enum_entites.WalletTypeEnum;
import com.starbuck.moneytracker.repository.WalletRepository;
import com.starbuck.moneytracker.service.WalletService;
import com.starbuck.moneytracker.util.CurrentUserUtil;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private WalletRepository walletRepo;

    @Mock
    private MessageSource messageSource;

    @Mock
    private CurrentUserUtil currentUser;

    private WalletService walletService;

    private final User user = new User(1L, "name", "password", "email");

    private final Wallet wallet = new Wallet(1L, "wallet", user, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT);

    @BeforeEach
    void setUp() {
        walletService = new WalletService(walletRepo, messageSource, currentUser);
        when(currentUser.getUser()).thenReturn(user);
    }

    @Test
    void testGetWalletById() {
        when(walletRepo.getWalletById(1L, 1L)).thenReturn(Optional.of(wallet));

        assertSame(wallet, walletService.getWalletById(1L));
    }

    @Test
    void testGetWalletByIdNotExistingWallet() {
        when(walletRepo.getWalletById(5L, 1L)).thenReturn(Optional.empty());

        assertThrowsExactly(EntityNotFoundException.class, () -> {
            walletService.getWalletById(5L);
        });
    }

    @Test
    void testGetBalanceForWallet() {
        when(walletRepo.getWalletById(1L, 1L)).thenReturn(Optional.of(wallet));
        when(walletRepo.getBalanceOfWallet(1L, 1L)).thenReturn(new BigDecimal("60.00"));

        assertEquals(new BigDecimal("60.00"), walletService.getBalanceForWallet(1L));
    }

    @Test
    void testGetBalanceForWalletNotExistingWallet() {
        when(walletRepo.getWalletById(5L, 1L)).thenReturn(Optional.empty());

        assertThrowsExactly(EntityNotFoundException.class, () -> {
            walletService.getBalanceForWallet(5L);
        });
        verify(walletRepo, never()).getBalanceOfWallet(anyLong(), anyLong());
    }
}
