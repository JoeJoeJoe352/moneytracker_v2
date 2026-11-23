package com.starbuck.moneytracker.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;
import com.starbuck.moneytracker.repository.BalanceSyncRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.util.CurrentUserUtil;

/**
 * A createTransactionPair nem publikus, ezért a teszt a service csomagban van
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

    private TransactionSaveCommand createCommand(TransactionTypeEnum type, String price, Long walletId) {
        return new TransactionSaveCommand(
                "transfer", new BigDecimal(price), LocalDate.of(2026, 11, 1), type, List.of(), List.of(), walletId);
    }
}
