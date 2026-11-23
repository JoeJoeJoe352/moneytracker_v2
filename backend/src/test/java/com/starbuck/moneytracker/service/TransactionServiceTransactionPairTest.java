package com.starbuck.moneytracker.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enums.CurrencyEnum;
import com.starbuck.moneytracker.entity.enums.GeneralStatusEnum;
import com.starbuck.moneytracker.entity.enums.TransactionSpecialTypeEnum;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;
import com.starbuck.moneytracker.entity.enums.WalletTypeEnum;
import com.starbuck.moneytracker.repository.BalanceSyncRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.util.CurrentUserUtil;

/**
 * A createTransactionPair és az updateTransactionPair nem publikus, ezért a
 * teszt a service csomagban van
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTransactionPairTest {

    @Mock
    private TransactionRepository transactionRepo;

    @Mock
    private TransactionDetailService transactionDetailService;

    @Mock
    private CurrentUserUtil currentUser;

    @Mock
    private WalletService walletService;

    @Mock
    private BalanceSyncRepository balanceSyncRepo;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                transactionRepo, transactionDetailService, currentUser, walletService, balanceSyncRepo);
    }

    /**
     * Ha a forrás oldal nem TRANSFER típusú, kivétel keletkezik és semmi nem
     * mentődik
     */
    @Test
    void createTransactionPair_throwsWhenSrcIsNotTransfer() {
        TransactionSaveCommand src = createCommand(TransactionTypeEnum.OUTCOME, "-10.00", 1L);
        TransactionSaveCommand dst =
                createCommand(TransactionTypeEnum.INCOME, "10.00", 2L).setTransferSpecialType();

        assertThrows(IllegalArgumentException.class, () -> transactionService.createTransactionPair(src, dst));

        Mockito.verify(transactionRepo, Mockito.never()).save(any(Transaction.class));
        Mockito.verifyNoInteractions(transactionDetailService, walletService);
    }

    /**
     * Ha a cél oldal nem TRANSFER típusú, kivétel keletkezik és semmi nem mentődik
     * (a forrás oldal sem)
     */
    @Test
    void createTransactionPair_throwsWhenDstIsNotTransfer() {
        TransactionSaveCommand src =
                createCommand(TransactionTypeEnum.OUTCOME, "-10.00", 1L).setTransferSpecialType();
        TransactionSaveCommand dst = createCommand(TransactionTypeEnum.INCOME, "10.00", 2L);

        assertThrows(IllegalArgumentException.class, () -> transactionService.createTransactionPair(src, dst));

        Mockito.verify(transactionRepo, Mockito.never()).save(any(Transaction.class));
        Mockito.verifyNoInteractions(transactionDetailService, walletService);
    }

    /**
     * Ha valamelyik parancs nem TRANSFER típusú, kivétel keletkezik, és a
     * tranzakciók be sem töltődnek
     */
    @Test
    void updateTransactionPair_throwsWhenCommandIsNotTransfer() {
        TransactionSaveCommand src =
                createCommand(TransactionTypeEnum.OUTCOME, "-10.00", 1L).setTransferSpecialType();
        TransactionSaveCommand dst = createCommand(TransactionTypeEnum.INCOME, "10.00", 2L);

        assertThrows(
                IllegalArgumentException.class, () -> transactionService.updateTransactionPair(10L, src, 11L, dst));

        Mockito.verifyNoInteractions(transactionRepo, transactionDetailService);
    }

    /**
     * Nem TRANSFER típusú tranzakció nem módosítható párként, és semmi nem
     * mentődik
     */
    @Test
    void updateTransactionPair_throwsWhenTransactionIsNotTransfer() {
        mockTransactionForUser(10L, null, 1L);
        TransactionSaveCommand src =
                createCommand(TransactionTypeEnum.OUTCOME, "-10.00", 1L).setTransferSpecialType();
        TransactionSaveCommand dst =
                createCommand(TransactionTypeEnum.INCOME, "10.00", 2L).setTransferSpecialType();

        assertThrows(
                IllegalArgumentException.class, () -> transactionService.updateTransactionPair(10L, src, 11L, dst));

        Mockito.verify(transactionRepo, Mockito.never()).save(any(Transaction.class));
        Mockito.verifyNoInteractions(transactionDetailService);
    }

    /**
     * Az átvezetés walletje nem változhat. Ha a cél oldalon eltér, a forrás oldal
     * sem módosul
     */
    @Test
    void updateTransactionPair_throwsWhenWalletChanges() {
        mockTransactionForUser(10L, TransactionSpecialTypeEnum.TRANSFER, 1L);
        mockTransactionForUser(11L, TransactionSpecialTypeEnum.TRANSFER, 2L);
        TransactionSaveCommand src =
                createCommand(TransactionTypeEnum.OUTCOME, "-10.00", 1L).setTransferSpecialType();
        TransactionSaveCommand dst =
                createCommand(TransactionTypeEnum.INCOME, "10.00", 3L).setTransferSpecialType();

        assertThrows(
                IllegalArgumentException.class, () -> transactionService.updateTransactionPair(10L, src, 11L, dst));

        Mockito.verify(transactionRepo, Mockito.never()).save(any(Transaction.class));
        Mockito.verifyNoInteractions(transactionDetailService);
    }

    /**
     * A bejelentkezett user (id: 1) adott id-jú tranzakcióját állítja be a mockon
     */
    private void mockTransactionForUser(Long id, TransactionSpecialTypeEnum specialType, Long walletId) {
        User user = new User(1L, "name", "password", "email");
        Wallet wallet = new Wallet("wallet", user, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT);
        wallet.setId(walletId);
        Transaction transaction = new Transaction(
                id,
                "transfer",
                LocalDate.of(2026, 11, 1),
                TransactionTypeEnum.OUTCOME,
                specialType,
                new BigDecimal("-10.00"),
                GeneralStatusEnum.ACTIVE,
                wallet);

        Mockito.when(currentUser.getUser()).thenReturn(user);
        Mockito.when(transactionRepo.getTransactionByIdWithDetails(id, 1L)).thenReturn(Optional.of(transaction));
    }

    private TransactionSaveCommand createCommand(TransactionTypeEnum type, String price, Long walletId) {
        return new TransactionSaveCommand(
                "transfer", new BigDecimal(price), LocalDate.of(2026, 11, 1), type, List.of(), List.of(), walletId);
    }
}
