package com.starbuck.moneytracker.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;

import com.starbuck.moneytracker.commands.TransferTransactionCreateCommand;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransferTransactionSide;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enums.CurrencyEnum;
import com.starbuck.moneytracker.entity.enums.TransactionSpecialTypeEnum;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;
import com.starbuck.moneytracker.entity.enums.WalletTypeEnum;
import com.starbuck.moneytracker.exception.EntityNotFoundException;
import com.starbuck.moneytracker.repository.BalanceSyncRepository;
import com.starbuck.moneytracker.repository.CategoryRepository;
import com.starbuck.moneytracker.repository.TransactionDetailCategoryRepository;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.repository.TransferTransactionSidesRepository;
import com.starbuck.moneytracker.service.CategoryService;
import com.starbuck.moneytracker.service.TransactionDetailService;
import com.starbuck.moneytracker.service.TransactionService;
import com.starbuck.moneytracker.service.TransferTransactionService;
import com.starbuck.moneytracker.service.WalletService;
import com.starbuck.moneytracker.util.CurrentUserUtil;
import com.starbuck.moneytracker.util.TransactionDetailFactory;

@ExtendWith(MockitoExtension.class)
class TransferTransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepo;

    @Mock
    private TransactionDetailRepository transactionDetailRepo;

    @Mock
    private TransactionDetailCategoryRepository transactionDetailCategoryRepository;

    @Mock
    private CategoryRepository categoryRepo;

    @Mock
    private ConversionService conversionService;

    @Mock
    private CurrentUserUtil currentUser;

    @Mock
    private WalletService walletService;

    @Mock
    private BalanceSyncRepository balanceSyncRepo;

    @Mock
    private TransferTransactionSidesRepository transferSidesRepo;

    @Spy
    private TransactionDetailFactory detailFactory = new TransactionDetailFactory();

    private TransferTransactionService transferService;

    private Wallet srcWallet;
    private Wallet dstWallet;

    @BeforeEach
    void setUp() {
        // Valódi TransactionService a mockolt repókkal, így a két oldal tényleges
        // létrehozása is tesztelve van
        var categoryService = new CategoryService(categoryRepo, currentUser, conversionService);
        var transactionDetailService = new TransactionDetailService(
                transactionDetailRepo, categoryService, transactionDetailCategoryRepository, detailFactory);
        var transactionService = new TransactionService(
                transactionRepo, transactionDetailService, currentUser, walletService, balanceSyncRepo);
        transferService = new TransferTransactionService(transactionService, transferSidesRepo);

        User user = new User(1L, "name", "password", "email");
        srcWallet = new Wallet("src", user, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT);
        dstWallet = new Wallet("dst", user, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT);

        Mockito.lenient().when(walletService.getWalletById(1L)).thenReturn(srcWallet);
        Mockito.lenient().when(walletService.getWalletById(2L)).thenReturn(dstWallet);
        Mockito.lenient()
                .when(transactionRepo.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.lenient()
                .when(transferSidesRepo.save(any(TransferTransactionSide.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * Átvezetéskor a forrás walletben kiadás, a cél walletben bevétel jön létre,
     * mindkettő TRANSFER típussal, és az összekötés is elmentődik
     */
    @Test
    void createLinkedTransaction_createsBothSidesAndLink() {
        TransferTransactionSide result = transferService.createLinkedTransaction(createCommand());

        Transaction src = result.getSrcTransaction();
        assertEquals("to savings", src.getName());
        assertSame(srcWallet, src.getWallet());
        assertEquals(TransactionTypeEnum.OUTCOME, src.getTransactionType());
        assertEquals(TransactionSpecialTypeEnum.TRANSFER, src.getSpecialType());
        assertEquals(0, new BigDecimal("-100.00").compareTo(src.getPriceSum()));
        assertEquals(LocalDate.of(2026, 11, 1), src.getTransactionDate());

        Transaction dst = result.getDstTransaction();
        assertEquals("from main", dst.getName());
        assertSame(dstWallet, dst.getWallet());
        assertEquals(TransactionTypeEnum.INCOME, dst.getTransactionType());
        assertEquals(TransactionSpecialTypeEnum.TRANSFER, dst.getSpecialType());
        assertEquals(0, new BigDecimal("100.00").compareTo(dst.getPriceSum()));
        assertEquals(LocalDate.of(2026, 11, 1), dst.getTransactionDate());

        Mockito.verify(transactionRepo, Mockito.times(2)).save(any(Transaction.class));
        Mockito.verify(transferSidesRepo).save(result);
    }

    /**
     * Ha a cél wallet nem a useré, kivétel keletkezik és az összekötés nem
     * mentődik (a forrás oldal a tranzakció visszagörgetésével tűnik el)
     */
    @Test
    void createLinkedTransaction_throwsAndSavesNoLinkWhenDstWalletNotFound() {
        Mockito.when(walletService.getWalletById(2L)).thenThrow(new EntityNotFoundException("Wallet not found: 2"));

        assertThrows(EntityNotFoundException.class, () -> {
            transferService.createLinkedTransaction(createCommand());
        });

        Mockito.verifyNoInteractions(transferSidesRepo);
    }

    private TransferTransactionCreateCommand createCommand() {
        return new TransferTransactionCreateCommand(
                "to savings", 1L, "from main", 2L, LocalDate.of(2026, 11, 1), new BigDecimal("100.00"));
    }
}
