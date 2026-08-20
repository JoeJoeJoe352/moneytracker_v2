package com.starbuck.moneytracker.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.starbuck.moneytracker.commands.TransactionCreateCommand;
import com.starbuck.moneytracker.commands.TransactionDetailSaveCommand;
import com.starbuck.moneytracker.commands.TransactionUpdateCommand;
import com.starbuck.moneytracker.dto.HistoryQueryHelperDto;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.TransactionDetailCategory;
import com.starbuck.moneytracker.entity.TransactionFilter;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;
import com.starbuck.moneytracker.repository.TransactionDetailCategoryRepository;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.service.TransactionService;
import com.starbuck.moneytracker.testutils.AssertUtil;
import com.starbuck.moneytracker.util.CurrentUserUtil;
import com.starbuck.moneytracker.util.TransactionDetailFactory;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepo;

    @Mock
    private TransactionDetailRepository transactionDetailRepo;

    @Mock
    private TransactionDetailCategoryRepository transactionDetailCategoryRepository;

    @Mock
    private CurrentUserUtil currentUser;

    @Mock
    private TransactionDetailFactory detailFactory;

    @InjectMocks
    private TransactionService transactionService;

    private AssertUtil assertUtil;

    TransactionServiceTest() {
        this.assertUtil = new AssertUtil();
    }

    @BeforeEach
    void createTransactionSaveMock() {
        // ha a save meghívódik, akkor visszaadja a paraméterben kapott tranzakciót +
        // szimuláljuk egy id beírását
        Mockito.lenient().when(transactionRepo.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction invocatedTransaction = invocation.getArgument(0);
            if (invocatedTransaction.getId() == null) {
                invocatedTransaction.setId(1L);
            }
            return invocatedTransaction;
        });
    }

    /**
     * Teszteli a tranzakciót és egy hozzá tartozó detail elmentését
     */
    @Test
    void createTransaction_savesTransactionAndDetail() {
        // GIVEN - előkészületek
        TransactionDetailSaveCommand detailCommand = new TransactionDetailSaveCommand(
                TransactionDetail.DEFAULT_DETAIL_NAME, new BigDecimal(100), List.of(), TransactionTypeEnum.INCOME);
        TransactionCreateCommand command = new TransactionCreateCommand("simpleTransaction", null, LocalDate.now(),
                TransactionTypeEnum.INCOME, List.of(detailCommand), List.of());

        ArgumentCaptor<TransactionDetail> captor = ArgumentCaptor.forClass(TransactionDetail.class);

        // WHEN - függvény meghívása
        Transaction result = transactionService.createTransaction(command);

        // THEN
        // Az elmentett detail példányt csak így tudjuk megszerezni
        Mockito.verify(transactionDetailRepo).save(captor.capture());

        assertUtil.assertTransaction(result, "simpleTransaction", LocalDate.now(), new BigDecimal("100.00"),
                TransactionTypeEnum.INCOME);
        assertUtil.assertDetail(captor.getValue(), TransactionDetail.DEFAULT_DETAIL_NAME,
                new BigDecimal("100.00"), null,
                null,
                result);
    }

    /**
     * Teszteli egy tranzakció és több detail elmentését
     */
    @Test
    void createTransaction_savesTransactionAndMultipleDetail() {
        // GIVEN
        TransactionDetailSaveCommand detail = new TransactionDetailSaveCommand("detail1", new BigDecimal(100),
                List.of(), TransactionTypeEnum.INCOME);
        TransactionDetailSaveCommand detail2 = new TransactionDetailSaveCommand("detail2", new BigDecimal(200),
                List.of(), TransactionTypeEnum.INCOME);
        TransactionCreateCommand command = new TransactionCreateCommand("multipleDetailedTransaction", null,
                LocalDate.now(), TransactionTypeEnum.INCOME, List.of(detail, detail2), List.of());

        ArgumentCaptor<TransactionDetail> captor = ArgumentCaptor.forClass(TransactionDetail.class);

        // WHEN
        Transaction result = transactionService.createTransaction(command);

        // THEN
        // tranzakció és a két detail is el lett mentve
        assertUtil.assertTransaction(result, "multipleDetailedTransaction", LocalDate.now(),
                new BigDecimal("300.00"),
                TransactionTypeEnum.INCOME);

        Mockito.verify(transactionDetailRepo, times(2)).save(captor.capture());

        var details = captor.getAllValues();

        assertUtil.assertDetail(details.get(0), "detail1", new BigDecimal("100.00"), null, null, result);
        assertUtil.assertDetail(details.get(1), "detail2", new BigDecimal("200.00"), null, null, result);
    }

    /**
     * Ha nincs egy detail sem megadva, alapértelmezett nevű detailt hoz létre
     * a tranzakció összegével
     */
    @Test
    void createTransaction_withNoDetails_createsDefaultDetail() {
        TransactionCreateCommand command = new TransactionCreateCommand("noDetailTransaction", null,
                LocalDate.now(), TransactionTypeEnum.INCOME, List.of(), List.of());

        Mockito.when(detailFactory.createDefauldDetail(BigDecimal.ZERO))
                .thenReturn(new TransactionDetail(TransactionDetail.DEFAULT_DETAIL_NAME, BigDecimal.ZERO));

        ArgumentCaptor<TransactionDetail> captor = ArgumentCaptor.forClass(TransactionDetail.class);

        Transaction result = transactionService.createTransaction(command);

        assertUtil.assertTransaction(result, "noDetailTransaction", LocalDate.now(), BigDecimal.ZERO,
                TransactionTypeEnum.INCOME);

        Mockito.verify(transactionDetailRepo).save(captor.capture());
        assertEquals(TransactionDetail.DEFAULT_DETAIL_NAME, captor.getValue().getName());
        assertEquals(result, captor.getValue().getTransaction());
    }

    /**
     * Ha egy detail neve üres, a command létrehozásakor hibát dob
     */
    @Test
    void createTransaction_throwsWhenDetailHasNoName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TransactionDetailSaveCommand("", new BigDecimal(100), List.of(), TransactionTypeEnum.INCOME);
        });
    }

    /**
     * Tranzakció frissítése, ha több tranzakciótétel van.
     */
    @Test
    void testUpdateTransaction() {
        // GIVEN
        // Ezeket úgy kezeljük, mintha már a db-ben lennének
        User userInDB = new User(1l, "alma", "pass", "email");
        Transaction transactionInDB = new Transaction(1l, "teszt", LocalDate.now(), TransactionTypeEnum.INCOME,
                new BigDecimal(100), 0);
        TransactionDetail detailInDB = new TransactionDetail(1l, "detail1", new BigDecimal(100), null, null,
                transactionInDB);
        transactionInDB.setTransactionDetails(List.of(detailInDB));

        // Ezek lesznek azok, amikkel módosítjuk az adatokat
        TransactionDetailSaveCommand updatedDetailCommand = new TransactionDetailSaveCommand("updatedDetail",
                new BigDecimal(-200), List.of(), TransactionTypeEnum.OUTCOME);
        TransactionDetailSaveCommand updatedDetailCommand2 = new TransactionDetailSaveCommand("updatedDetail2",
                new BigDecimal(-300), List.of(), TransactionTypeEnum.OUTCOME);
        TransactionUpdateCommand updateCommand = new TransactionUpdateCommand("updated", null,
                LocalDate.of(2023, 1, 1), TransactionTypeEnum.OUTCOME,
                List.of(updatedDetailCommand, updatedDetailCommand2), List.of());

        Mockito.when(currentUser.getUser()).thenReturn(userInDB);
        Mockito.when(transactionRepo.getTransactionByIdWithDetails(anyLong(), anyLong()))
                .thenReturn(Optional.of(transactionInDB));

        ArgumentCaptor<Transaction> captorTransaction = ArgumentCaptor.forClass(Transaction.class);
        ArgumentCaptor<TransactionDetail> captorDetail = ArgumentCaptor.forClass(TransactionDetail.class);

        // WHEN
        transactionService.updateTransaction(1l, updateCommand);

        // THEN - az új beállított értékeket menti el
        Mockito.verify(transactionRepo).save(captorTransaction.capture());
        Transaction savedTransaction = captorTransaction.getValue();
        assertUtil.assertTransaction(savedTransaction, "updated", LocalDate.of(2023, 1, 1),
                new BigDecimal("-500.00"),
                TransactionTypeEnum.OUTCOME);

        // törli az eddigi detailokat és elmenti az újakat
        Mockito.verify(transactionDetailRepo).deleteAll(List.of(detailInDB));
        Mockito.verify(transactionDetailRepo, times(2)).save(captorDetail.capture());

        var details = captorDetail.getAllValues();
        assertUtil.assertDetail(details.get(0), "updatedDetail", new BigDecimal("-200.00"), null, null,
                savedTransaction);
        assertUtil.assertDetail(details.get(1), "updatedDetail2", new BigDecimal("-300.00"), null, null,
                savedTransaction);
    }

    /**
     * Tranzakció mentése, weight + unitprice-os detail-al
     */
    @Test
    void testCreateTransactionWithWeightAndUnitPrice() {
        // GIVEN
        TransactionDetailSaveCommand detail1 = new TransactionDetailSaveCommand("WeightesDetail",
                new BigDecimal("0.5"), new BigDecimal("300"), List.of());
        TransactionDetailSaveCommand detail2 = new TransactionDetailSaveCommand("Simadetail", new BigDecimal("200"),
                List.of(), TransactionTypeEnum.INCOME);
        TransactionCreateCommand createCommand = new TransactionCreateCommand(
                "multipleDetailedTransactionWithWeightAndUnitPrice", null, LocalDate.now(),
                TransactionTypeEnum.INCOME, List.of(detail1, detail2), List.of());

        ArgumentCaptor<TransactionDetail> captor = ArgumentCaptor.forClass(TransactionDetail.class);

        // WHEN
        var result = transactionService.createTransaction(createCommand);

        // THEN
        // Tranzakció elmentve és helyesek az adatai
        assertUtil.assertTransaction(result, "multipleDetailedTransactionWithWeightAndUnitPrice",
                LocalDate.now(),
                new BigDecimal("350.00"),
                TransactionTypeEnum.INCOME);

        // detailok elmentve és helyesek az adatai
        Mockito.verify(transactionDetailRepo, times(2)).save(captor.capture());
        var details = captor.getAllValues();

        assertUtil.assertDetail(details.get(0), "WeightesDetail", new BigDecimal("150.00"),
                null, null,
                result);
        assertUtil.assertDetail(details.get(1), "Simadetail", new BigDecimal("200.00"), null, null, result);
    }

    @Test
    void testUpdateTransactionWithWeightAndUnitPrice() {
        // GIVEN
        // "Db-ben" lévő dolgok
        User userInDB = new User(1l, "alma", "pass", "email");

        Transaction transactionInDB = new Transaction("multipleDetailedTransactionWithWeightAndUnitPrice",
                LocalDate.now(),
                TransactionTypeEnum.INCOME,
                null);

        TransactionDetail detail1inDb = new TransactionDetail("WeightesDetail1", new BigDecimal("0.5"),
                new BigDecimal(300));
        TransactionDetail detail2inDb = new TransactionDetail("Simadetail1", new BigDecimal(200));
        transactionInDB.setTransactionDetails(List.of(detail1inDb, detail2inDb));

        // updatelő dolgok
        // weightesből sima priceos, priceosból weightes tranzakciót csinálunk
        TransactionDetailSaveCommand updatedDetail = new TransactionDetailSaveCommand("simadetail1",
                new BigDecimal(-200), List.of(), TransactionTypeEnum.OUTCOME);
        TransactionDetailSaveCommand updatedDetail2 = new TransactionDetailSaveCommand("weightresDetail2",
                new BigDecimal("0.7"), new BigDecimal(300), List.of());
        TransactionUpdateCommand updatedTransaction = new TransactionUpdateCommand("updated", null,
                LocalDate.of(2023, 1, 1), TransactionTypeEnum.OUTCOME, List.of(updatedDetail, updatedDetail2),
                List.of());

        ArgumentCaptor<Transaction> captorTransaction = ArgumentCaptor.forClass(Transaction.class);
        ArgumentCaptor<TransactionDetail> captorDetails = ArgumentCaptor.forClass(TransactionDetail.class);

        Mockito.when(currentUser.getUser()).thenReturn(userInDB);
        Mockito.when(transactionRepo.getTransactionByIdWithDetails(anyLong(), anyLong()))
                .thenReturn(Optional.of(transactionInDB));

        // WHEN
        transactionService.updateTransaction(1l, updatedTransaction);

        // THEN
        // Tranzakció elmentve és helyesek az adatai
        Mockito.verify(transactionRepo).save(captorTransaction.capture());
        var transaction = captorTransaction.getValue();
        this.assertUtil.assertTransaction(transaction, "updated", LocalDate.of(2023, 1, 1),
                new BigDecimal("-410.00"), TransactionTypeEnum.OUTCOME);

        // detailok elmentve és helyesek az adatai
        Mockito.verify(transactionDetailRepo, times(2)).save(captorDetails.capture());
        var details = captorDetails.getAllValues();
        assertUtil.assertDetail(details.get(0), "simadetail1", new BigDecimal("-200.00"), null, null,
                transaction);
        assertUtil.assertDetail(details.get(1), "weightresDetail2", new BigDecimal("-210.00"),
                null, null,
                transaction);
    }

    /**
     * Ha a detailhez kategória kapcsolat is meg van adva, azt is elmenti a
     * createTransaction
     */
    @Test
    void createTransaction_savesCategoryLinksForDetail() {
        // GIVEN
        TransactionDetailSaveCommand detail = new TransactionDetailSaveCommand("detailWithCategory",
                new BigDecimal(100), List.of(5L), TransactionTypeEnum.INCOME);
        TransactionCreateCommand command = new TransactionCreateCommand("categorizedTransaction", null,
                LocalDate.now(), TransactionTypeEnum.INCOME, List.of(detail), List.of());

        // WHEN
        transactionService.createTransaction(command);

        // THEN
        ArgumentCaptor<TransactionDetailCategory> captor = ArgumentCaptor
                .forClass(TransactionDetailCategory.class);
        Mockito.verify(transactionDetailCategoryRepository).save(captor.capture());
        assertEquals(5L, captor.getValue().getCategory().getId());
    }

    /**
     * Income tranzakciónál negatív/nulla összegű detail hibát dob
     */
    @Test
    void createTransaction_throwsWhenIncomeDetailPriceNotPositive() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TransactionDetailSaveCommand(TransactionDetail.DEFAULT_DETAIL_NAME, new BigDecimal(-50),
                    List.of(), TransactionTypeEnum.INCOME);
        });
    }

    /**
     * Outcome tranzakciónál pozitív/nulla összegű detail hibát dob
     */
    @Test
    void createTransaction_throwsWhenOutcomeDetailPriceNotNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TransactionDetailSaveCommand(TransactionDetail.DEFAULT_DETAIL_NAME, new BigDecimal(50),
                    List.of(), TransactionTypeEnum.OUTCOME);
        });
    }

    /**
     * Ha se weight, se unitPrice nincs megadva, hibát dob
     */
    @Test
    void createTransaction_throwsWhenOnlyWeightIsSetWithoutUnitPrice() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TransactionDetailSaveCommand("badDetail", new BigDecimal("0.5"), null, List.of());
        });
    }

    /**
     * Frissítéskor hibát dob a command létrehozásakor, ha nincs egy detail sem
     * megadva
     */
    @Test
    void updateTransaction_throwsWhenNoDetailsProvided() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TransactionUpdateCommand("teszt", null, LocalDate.now(), TransactionTypeEnum.INCOME, List.of(),
                    List.of());
        });
    }

    /**
     * Ha a tranzakció nem található (más useré, vagy nem létezik), hibát dob
     */
    @Test
    void updateTransaction_throwsWhenTransactionNotFoundForUser() {
        User userInDB = new User(1l, "alma", "pass", "email");
        TransactionDetailSaveCommand updatedDetail = new TransactionDetailSaveCommand("detail", new BigDecimal(100),
                List.of(), TransactionTypeEnum.INCOME);
        TransactionUpdateCommand updateCommand = new TransactionUpdateCommand("updated", null, LocalDate.now(),
                TransactionTypeEnum.INCOME, List.of(updatedDetail), List.of());

        Mockito.when(currentUser.getUser()).thenReturn(userInDB);
        Mockito.when(transactionRepo.getTransactionByIdWithDetails(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            transactionService.updateTransaction(1l, updateCommand);
        });
    }

    /**
     * Összegzi a user összes pénzét
     */
    @Test
    void sumAllMoney_returnsSumFromRepository() {
        User userInDB = new User(1l, "alma", "pass", "email");
        Mockito.when(currentUser.getUser()).thenReturn(userInDB);
        Mockito.when(transactionRepo.summarizeTotalMoneyForUser(1l)).thenReturn(new BigDecimal("1234.56"));

        BigDecimal result = transactionService.sumAllMoney();

        assertEquals(new BigDecimal("1234.56"), result);
    }

    /**
     * Ha még nincs egy tranzakciója sem a usernek, nullát ad vissza null helyett
     */
    @Test
    void sumAllMoney_returnsZeroWhenRepositoryReturnsNull() {
        User userInDB = new User(1l, "alma", "pass", "email");
        Mockito.when(currentUser.getUser()).thenReturn(userInDB);
        Mockito.when(transactionRepo.summarizeTotalMoneyForUser(1l)).thenReturn(null);

        BigDecimal result = transactionService.sumAllMoney();

        assertEquals(BigDecimal.ZERO, result);
    }

    /**
     * Kiszámolja a havi kiadás összegét
     */
    @Test
    void sumAllExpenseForMonth_returnsSumForOutcomeType() {
        User userInDB = new User(1l, "alma", "pass", "email");
        Mockito.when(currentUser.getUser()).thenReturn(userInDB);
        Mockito.when(transactionRepo.summarizeTransactionPricesForMonthAndType(1l, TransactionTypeEnum.OUTCOME))
                .thenReturn(new BigDecimal("-300.00"));

        BigDecimal result = transactionService.sumAllExpenseForMonth();

        assertEquals(new BigDecimal("-300.00"), result);
    }

    /**
     * Ha nincs kiadás a hónapban, nullát ad vissza null helyett
     */
    @Test
    void sumAllExpenseForMonth_returnsZeroWhenRepositoryReturnsNull() {
        User userInDB = new User(1l, "alma", "pass", "email");
        Mockito.when(currentUser.getUser()).thenReturn(userInDB);
        Mockito.when(transactionRepo.summarizeTransactionPricesForMonthAndType(1l, TransactionTypeEnum.OUTCOME))
                .thenReturn(null);

        BigDecimal result = transactionService.sumAllExpenseForMonth();

        assertEquals(BigDecimal.ZERO, result);
    }

    /**
     * Kiszámolja a havi bevétel összegét
     */
    @Test
    void sumAllIncomeForMonth_returnsSumForIncomeType() {
        User userInDB = new User(1l, "alma", "pass", "email");
        Mockito.when(currentUser.getUser()).thenReturn(userInDB);
        Mockito.when(transactionRepo.summarizeTransactionPricesForMonthAndType(1l, TransactionTypeEnum.INCOME))
                .thenReturn(new BigDecimal("500.00"));

        BigDecimal result = transactionService.sumAllIncomeForMonth();

        assertEquals(new BigDecimal("500.00"), result);
    }

    /**
     * Ha nincs bevétel a hónapban, nullát ad vissza null helyett
     */
    @Test
    void sumAllIncomeForMonth_returnsZeroWhenRepositoryReturnsNull() {
        User userInDB = new User(1l, "alma", "pass", "email");
        Mockito.when(currentUser.getUser()).thenReturn(userInDB);
        Mockito.when(transactionRepo.summarizeTransactionPricesForMonthAndType(1l, TransactionTypeEnum.INCOME))
                .thenReturn(null);

        BigDecimal result = transactionService.sumAllIncomeForMonth();

        assertEquals(BigDecimal.ZERO, result);
    }

    /**
     * Visszaadja az adott id-jú tranzakciót, ha a userhez tartozik
     */
    @Test
    void getTransactionByIdForActualUser_returnsTransactionWhenFound() {
        User userInDB = new User(1l, "alma", "pass", "email");
        Transaction transactionInDB = new Transaction(1l, "teszt", LocalDate.now(), TransactionTypeEnum.INCOME,
                new BigDecimal(100), 0);

        Mockito.when(currentUser.getUser()).thenReturn(userInDB);
        Mockito.when(transactionRepo.getTransactionByIdWithDetails(1l, 1l))
                .thenReturn(Optional.of(transactionInDB));

        Transaction result = transactionService.getTransactionByIdForActualUser(1l);

        assertEquals(transactionInDB, result);
    }

    /**
     * Hibát dob, ha nem található a tranzakció a userhez
     */
    @Test
    void getTransactionByIdForActualUser_throwsWhenNotFound() {
        User userInDB = new User(1l, "alma", "pass", "email");

        Mockito.when(currentUser.getUser()).thenReturn(userInDB);
        Mockito.when(transactionRepo.getTransactionByIdWithDetails(1l, 1l))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            transactionService.getTransactionByIdForActualUser(1l);
        });
    }

    /**
     * Az utolsó 5 tranzakciót kéri le, szűrés nélkül
     */
    @Test
    void getLastTransactions_queriesWithLimitOfFiveAndNoFilter() {
        User userInDB = new User(1l, "alma", "pass", "email");
        Transaction transactionInDB = new Transaction(1l, "teszt", LocalDate.now(), TransactionTypeEnum.INCOME,
                new BigDecimal(100), 0);

        Mockito.when(currentUser.getUser()).thenReturn(userInDB);
        Mockito.when(transactionRepo.findAllForUser(Mockito.eq(1l), any(HistoryQueryHelperDto.class)))
                .thenReturn(List.of(transactionInDB));

        List<Transaction> result = transactionService.getLastTransactions();

        ArgumentCaptor<HistoryQueryHelperDto> captor = ArgumentCaptor.forClass(HistoryQueryHelperDto.class);
        Mockito.verify(transactionRepo).findAllForUser(Mockito.eq(1l), captor.capture());

        assertEquals(List.of(transactionInDB), result);
        assertEquals(5, captor.getValue().limit());
    }

    /**
     * A tranzakciók oldalhoz 30-as limittel és a kapott szűréssel kéri le az
     * adatokat
     */
    @Test
    void getHistoryPageData_queriesWithLimitOfThirtyAndGivenFilter() {
        User userInDB = new User(1l, "alma", "pass", "email");
        Transaction transactionInDB = new Transaction(1l, "teszt", LocalDate.now(), TransactionTypeEnum.INCOME,
                new BigDecimal(100), 0);
        TransactionFilter filter = new TransactionFilter("teszt", null);

        Mockito.when(currentUser.getUser()).thenReturn(userInDB);
        Mockito.when(transactionRepo.findAllForUser(Mockito.eq(1l), any(HistoryQueryHelperDto.class)))
                .thenReturn(List.of(transactionInDB));

        List<Transaction> result = transactionService.getHistoryPageData(filter);

        ArgumentCaptor<HistoryQueryHelperDto> captor = ArgumentCaptor.forClass(HistoryQueryHelperDto.class);
        Mockito.verify(transactionRepo).findAllForUser(Mockito.eq(1l), captor.capture());

        assertEquals(List.of(transactionInDB), result);
        assertEquals(30, captor.getValue().limit());
    }

    /**
     * Törlésnél hibát dob, ha a tranzakció nem található/nem a userhez tartozik
     */
    @Test
    void deleteTransaction_throwsWhenTransactionNotFound() {
        User userInDB = new User(1l, "alma", "pass", "email");

        Mockito.when(currentUser.getUser()).thenReturn(userInDB);
        Mockito.when(transactionRepo.findById(1l))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            transactionService.deleteTransaction(1l);
        });

        Mockito.verify(transactionRepo, Mockito.never()).delete(any(Transaction.class));
    }
}