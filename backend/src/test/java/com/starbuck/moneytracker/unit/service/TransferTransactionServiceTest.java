package com.starbuck.moneytracker.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;

import com.starbuck.moneytracker.commands.TransferTransactionCreateCommand;
import com.starbuck.moneytracker.commands.TransferTransactionUpdateCommand;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransferTransactionSide;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enums.CurrencyEnum;
import com.starbuck.moneytracker.entity.enums.GeneralStatusEnum;
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
        srcWallet.setId(1L);
        dstWallet = new Wallet("dst", user, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT);
        dstWallet.setId(2L);

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

    /**
     * Különböző pénznemű walletek között nem lehet átvezetni: kivétel keletkezik,
     * és se a tranzakciók, se az összekötés nem mentődik
     */
    @Test
    void createLinkedTransaction_throwsWhenWalletCurrenciesDiffer() {
        dstWallet.setCurrencyCode(CurrencyEnum.EUR);

        assertThrows(IllegalArgumentException.class, () -> {
            transferService.createLinkedTransaction(createCommand());
        });

        Mockito.verify(transactionRepo, Mockito.never()).save(any(Transaction.class));
        Mockito.verifyNoInteractions(transactionDetailRepo, transferSidesRepo);
    }

    /**
     * Átvezetés módosításakor mindkét oldal új nevet, dátumot és előjeles összeget
     * kap, a walletek és a típusok változatlanok
     */
    @Test
    void updateLinkedTransaction_updatesBothSides() {
        TransferTransactionSide side = mockExistingTransfer();

        transferService.updateLinkedTransaction(7L, updateCommand());

        Transaction src = side.getSrcTransaction();
        assertEquals("new src", src.getName());
        assertEquals(LocalDate.of(2026, 11, 20), src.getTransactionDate());
        assertEquals(TransactionTypeEnum.OUTCOME, src.getTransactionType());
        assertEquals(TransactionSpecialTypeEnum.TRANSFER, src.getSpecialType());
        assertEquals(0, new BigDecimal("-250.00").compareTo(src.getPriceSum()));
        assertSame(srcWallet, src.getWallet());

        Transaction dst = side.getDstTransaction();
        assertEquals("new dst", dst.getName());
        assertEquals(LocalDate.of(2026, 11, 20), dst.getTransactionDate());
        assertEquals(TransactionTypeEnum.INCOME, dst.getTransactionType());
        assertEquals(TransactionSpecialTypeEnum.TRANSFER, dst.getSpecialType());
        assertEquals(0, new BigDecimal("250.00").compareTo(dst.getPriceSum()));
        assertSame(dstWallet, dst.getWallet());

        Mockito.verify(transactionRepo).save(src);
        Mockito.verify(transactionRepo).save(dst);
        Mockito.verify(transferSidesRepo, Mockito.never()).save(any(TransferTransactionSide.class));
    }

    /**
     * Nem létező átvezetés módosítása kivételt dob, és semmi nem mentődik
     */
    @Test
    void updateLinkedTransaction_throwsWhenTransferNotFound() {
        Mockito.when(transferSidesRepo.findActiveById(7L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            transferService.updateLinkedTransaction(7L, updateCommand());
        });

        Mockito.verify(transactionRepo, Mockito.never()).save(any(Transaction.class));
    }

    /**
     * Más user átvezetése nem módosítható: a tranzakció nem található a
     * bejelentkezett usernek, és semmi nem mentődik
     */
    @Test
    void updateLinkedTransaction_throwsWhenTransferBelongsToOtherUser() {
        mockExistingTransfer();
        Mockito.when(transactionRepo.getTransactionByIdWithDetails(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            transferService.updateLinkedTransaction(7L, updateCommand());
        });

        Mockito.verify(transactionRepo, Mockito.never()).save(any(Transaction.class));
        Mockito.verifyNoInteractions(transactionDetailRepo);
    }

    /**
     * Egy létező átvezetést (id: 7, forrás tranzakció: 10, cél tranzakció: 11)
     * állít be a mockokon, a bejelentkezett user (id: 1) nevében
     */
    private TransferTransactionSide mockExistingTransfer() {
        Transaction src = new Transaction(
                10L,
                "old src",
                LocalDate.of(2026, 11, 1),
                TransactionTypeEnum.OUTCOME,
                TransactionSpecialTypeEnum.TRANSFER,
                new BigDecimal("-100.00"),
                GeneralStatusEnum.ACTIVE,
                srcWallet);
        Transaction dst = new Transaction(
                11L,
                "old dst",
                LocalDate.of(2026, 11, 1),
                TransactionTypeEnum.INCOME,
                TransactionSpecialTypeEnum.TRANSFER,
                new BigDecimal("100.00"),
                GeneralStatusEnum.ACTIVE,
                dstWallet);
        TransferTransactionSide side = new TransferTransactionSide(src, dst);
        side.setId(7L);

        Mockito.lenient().when(currentUser.getUser()).thenReturn(new User(1L, "name", "password", "email"));
        Mockito.when(transferSidesRepo.findActiveById(7L)).thenReturn(Optional.of(side));
        // módosításhoz detailokkal, törléshez detailok nélkül töltődik be
        Mockito.lenient()
                .when(transactionRepo.getTransactionByIdWithDetails(10L, 1L))
                .thenReturn(Optional.of(src));
        Mockito.lenient()
                .when(transactionRepo.getTransactionByIdWithDetails(11L, 1L))
                .thenReturn(Optional.of(dst));
        Mockito.lenient().when(transactionRepo.findById(10L)).thenReturn(Optional.of(src));
        Mockito.lenient().when(transactionRepo.findById(11L)).thenReturn(Optional.of(dst));
        return side;
    }

    /**
     * Átvezetés törlésekor mindkét oldal törlődik, az összekötés megmarad
     */
    @Test
    void deleteLinkedTransaction_deletesBothSidesAndKeepsLink() {
        TransferTransactionSide side = mockExistingTransfer();

        transferService.deleteLinkedTransaction(7L);

        Mockito.verify(transactionRepo).delete(side.getSrcTransaction());
        Mockito.verify(transactionRepo).delete(side.getDstTransaction());
        Mockito.verify(transferSidesRepo, Mockito.never()).delete(any(TransferTransactionSide.class));
    }

    /**
     * Nem létező (vagy már törölt) átvezetés törlése kivételt dob, és semmi nem
     * törlődik
     */
    @Test
    void deleteLinkedTransaction_throwsWhenTransferNotFound() {
        Mockito.when(transferSidesRepo.findActiveById(7L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            transferService.deleteLinkedTransaction(7L);
        });

        Mockito.verify(transactionRepo, Mockito.never()).delete(any(Transaction.class));
    }

    /**
     * Más user átvezetése nem törölhető: ha a cél oldal nem a bejelentkezett useré,
     * a forrás oldal sem törlődik
     */
    @Test
    void deleteLinkedTransaction_throwsWhenTransferBelongsToOtherUser() {
        mockExistingTransfer();
        Mockito.when(transactionRepo.findById(11L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            transferService.deleteLinkedTransaction(7L);
        });

        Mockito.verify(transactionRepo, Mockito.never()).delete(any(Transaction.class));
    }

    private TransferTransactionUpdateCommand updateCommand() {
        return new TransferTransactionUpdateCommand(
                "new src", "new dst", LocalDate.of(2026, 11, 20), new BigDecimal("250.00"));
    }

    private TransferTransactionCreateCommand createCommand() {
        return new TransferTransactionCreateCommand(
                "to savings", 1L, "from main", 2L, LocalDate.of(2026, 11, 1), new BigDecimal("100.00"));
    }
}
