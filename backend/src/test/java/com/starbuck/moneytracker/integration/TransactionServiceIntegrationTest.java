package com.starbuck.moneytracker.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.starbuck.moneytracker.commands.TransactionCreateCommand;
import com.starbuck.moneytracker.commands.TransactionDetailSaveCommand;
import com.starbuck.moneytracker.commands.TransactionUpdateCommand;
import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.TransactionDetailCategory;
import com.starbuck.moneytracker.entity.TransactionFilter;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.enum_entites.LangEnum;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;
import com.starbuck.moneytracker.repository.CategoryRepository;
import com.starbuck.moneytracker.repository.TransactionDetailCategoryRepository;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.repository.UserRepository;
import com.starbuck.moneytracker.service.TransactionService;
import com.starbuck.moneytracker.util.CurrentUserUtil;

import jakarta.persistence.EntityNotFoundException;

@SpringBootTest
@ActiveProfiles("test")
// csak így használható a beforeall, mert egyébként statikusan futna és nem
// elérhető az injektált dolgok
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransactionServiceIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepo;

    @Autowired
    private CategoryRepository categoryRepo;

    @Autowired
    private TransactionDetailCategoryRepository detailCategoryRepo;

    @Autowired
    private TransactionDetailRepository transactionDetailRepo;

    @Autowired
    private UserRepository userRepo;

    @MockitoBean
    private CurrentUserUtil currentUser;

    private User user = null;

    @BeforeAll
    void beforeAll() {
        User user = new User("testuser", "password", "teszt@email.com");
        user.setUuid();
        this.user = userRepo.save(user);
    }

    @AfterAll
    void afterAll() {
        userRepo.delete(this.user);
    }

    /**
     * Létrehoz és megnézi, hogy bekerült-e minden jól a db-ben
     */
    @Test
    void createTransaction_persistsAllEntities() {
        // GIVEN
        Mockito.when(currentUser.getUser()).thenReturn(this.user);

        Category category = new Category("tesztkategória", this.user, LangEnum.HU);
        categoryRepo.save(category);

        TransactionDetailSaveCommand detailCommand = new TransactionDetailSaveCommand(
                TransactionDetail.DEFAULT_DETAIL_NAME, new BigDecimal("100.00"), List.of(category.getId()),
                TransactionTypeEnum.INCOME);
        TransactionCreateCommand command = new TransactionCreateCommand("Test", null, LocalDate.of(2026, 6, 8),
                TransactionTypeEnum.INCOME, List.of(detailCommand), List.of());

        // WHEN
        Transaction saved = transactionService.createTransaction(command);

        // THEN
        assertNotNull(saved.getId());

        TransactionDetail detail = transactionDetailRepo.findAll().get(0);
        assertEquals(TransactionDetail.DEFAULT_DETAIL_NAME, detail.getName());
        assertEquals(saved.getId(), detail.getTransaction().getId());

        TransactionDetailCategory detailCategory = detailCategoryRepo.findAll().get(0);
        assertEquals(detail.getId(), detailCategory.getTransactionDetail().getId());
        assertEquals(category.getId(), detailCategory.getCategory().getId());

        assertEquals(1, transactionRepo.count());
        assertEquals(1, transactionDetailRepo.count());
        assertEquals(1, detailCategoryRepo.count());
        assertEquals(new BigDecimal("100.00"), saved.getPriceSum());

        this.deleteData(saved);
    }

    @Test
    void updateTransaction_persistsAllEntities() {
        // GIVEN
        Mockito.when(currentUser.getUser()).thenReturn(this.user);

        Category category1 = new Category("tesztKategória", this.user, LangEnum.HU);
        categoryRepo.save(category1);

        TransactionDetailSaveCommand detailCommand = new TransactionDetailSaveCommand(
                TransactionDetail.DEFAULT_DETAIL_NAME, new BigDecimal("100.00"), List.of(category1.getId()),
                TransactionTypeEnum.INCOME);
        TransactionCreateCommand createCommand = new TransactionCreateCommand("Test", null,
                LocalDate.of(2026, 6, 8), TransactionTypeEnum.INCOME, List.of(detailCommand), List.of());

        // Van DB-ben elem mostmár
        Transaction createdTransaction = transactionService.createTransaction(createCommand);

        // Update elemek létrehozása
        Category category2 = new Category("updateKategória", this.user, LangEnum.HU);
        categoryRepo.save(category2);

        // Detail 1 beállításai - 1 kategóriája van
        TransactionDetailSaveCommand updateDetailCommand1 = new TransactionDetailSaveCommand("update1",
                new BigDecimal("-200.00"), List.of(category2.getId()), TransactionTypeEnum.OUTCOME);

        // Detail2 beállításai - 2 kategória beállítva
        TransactionDetailSaveCommand updateDetailCommand2 = new TransactionDetailSaveCommand("update2",
                new BigDecimal("0.5"), new BigDecimal("600.00"), List.of(category2.getId(), category1.getId()));

        TransactionUpdateCommand updateCommand = new TransactionUpdateCommand("Update test", null,
                LocalDate.of(2026, 7, 8), TransactionTypeEnum.OUTCOME,
                List.of(updateDetailCommand1, updateDetailCommand2), List.of());

        // WHEN
        transactionService.updateTransaction(createdTransaction.getId(), updateCommand);

        // THEN
        // Elemszámok validálása
        assertEquals(1, transactionRepo.count());
        assertEquals(2, transactionDetailRepo.count());
        assertEquals(3, detailCategoryRepo.count());
        assertEquals(2, categoryRepo.count());

        // Tranzakció validálása
        Transaction transactionFromDb = transactionRepo.findAll().get(0);
        assertEquals("Update test", transactionFromDb.getName());
        assertEquals(TransactionTypeEnum.OUTCOME, transactionFromDb.getTransactionType());
        assertEquals(LocalDate.of(2026, 7, 8), transactionFromDb.getTransactionDate());
        assertEquals(new BigDecimal("-500.00"), transactionFromDb.getPriceSum());

        // Detailok validálása
        List<TransactionDetail> detailsFromDb = transactionDetailRepo.findAll();
        var detailFromDb1 = detailsFromDb.get(0);
        var detailFromDb2 = detailsFromDb.get(1);
        assertEquals("update1", detailFromDb1.getName());
        assertEquals(new BigDecimal("-200.00"), detailFromDb1.getPrice());
        assertEquals(transactionFromDb.getId(), detailFromDb1.getTransaction().getId());
        assertEquals("update2", detailFromDb2.getName());
        assertEquals(new BigDecimal("-300.00"), detailFromDb2.getPrice());
        assertEquals(transactionFromDb.getId(), detailFromDb2.getTransaction().getId());

        // Kapcsolótábla bejegyzések validálása
        List<TransactionDetailCategory> detailCategories = detailCategoryRepo.findAll();
        assertEquals("update1", detailCategories.get(0).getTransactionDetail().getName());
        assertEquals("updateKategória", detailCategories.get(0).getCategory().getName());
        assertEquals("update2", detailCategories.get(1).getTransactionDetail().getName());
        assertEquals("updateKategória", detailCategories.get(1).getCategory().getName());
        assertEquals("update2", detailCategories.get(2).getTransactionDetail().getName());
        assertEquals("tesztKategória", detailCategories.get(2).getCategory().getName());

        this.deleteData(transactionFromDb);
    }

    @Test
    void createTransaction_throwsExceptionAndRollsBack() {
        Mockito.when(currentUser.getUser()).thenReturn(this.user);

        // emiatt nem fogja tudni elmenteni a detailst és rollback az egész
        TransactionDetailSaveCommand detailCommand = new TransactionDetailSaveCommand(
                "hosszunev0".repeat(26), new BigDecimal(100), List.of(), TransactionTypeEnum.INCOME);
        TransactionCreateCommand command = new TransactionCreateCommand("hibásteszt", null,
                LocalDate.of(2026, 6, 8), TransactionTypeEnum.INCOME, List.of(detailCommand), List.of());

        assertThrows(DataIntegrityViolationException.class, () -> {
            transactionService.createTransaction(command);
        });

        // rollback miatt nincs egy sem a db-ben
        assertEquals(0, transactionRepo.count());
        assertEquals(0, transactionDetailRepo.count());
    }

    /**
     * A user aktív tranzakcióinak összegét adja vissza
     */
    @Test
    void sumAllMoney_returnsSumOfActiveTransactions() {
        // GIVEN
        Mockito.when(currentUser.getUser()).thenReturn(this.user);

        Transaction income = this.persistSimpleTransaction("income", TransactionTypeEnum.INCOME,
                new BigDecimal(500), LocalDate.now());
        Transaction expense = this.persistSimpleTransaction("expense", TransactionTypeEnum.OUTCOME,
                new BigDecimal(-200), LocalDate.now());

        // WHEN
        BigDecimal result = transactionService.sumAllMoney();

        // THEN
        assertEquals(new BigDecimal("300.00"), result);

        this.deleteData(income);
        this.deleteData(expense);
    }

    /**
     * Ha nincs egy tranzakciója sem a usernek, nullát ad vissza
     */
    @Test
    void sumAllMoney_returnsZeroWhenNoTransactions() {
        Mockito.when(currentUser.getUser()).thenReturn(this.user);

        BigDecimal result = transactionService.sumAllMoney();

        assertEquals(BigDecimal.ZERO, result);
    }

    /**
     * A jelenlegi hónap kiadásainak összegét adja vissza, a más hónapbelieket
     * és a bevételeket figyelmen kívül hagyva
     */
    @Test
    void sumAllExpenseForMonth_returnsOnlyCurrentMonthExpenses() {
        Mockito.when(currentUser.getUser()).thenReturn(this.user);

        Transaction expenseThisMonth = this.persistSimpleTransaction("expenseThisMonth", TransactionTypeEnum.OUTCOME,
                new BigDecimal(-150), LocalDate.now());
        Transaction incomeThisMonth = this.persistSimpleTransaction("incomeThisMonth", TransactionTypeEnum.INCOME,
                new BigDecimal(150), LocalDate.now());
        Transaction expenseLastYear = this.persistSimpleTransaction("expenseLastYear", TransactionTypeEnum.OUTCOME,
                new BigDecimal(-999), LocalDate.now().minusYears(1));

        BigDecimal result = transactionService.sumAllExpenseForMonth();

        assertEquals(new BigDecimal("-150.00"), result);

        this.deleteData(expenseThisMonth);
        this.deleteData(incomeThisMonth);
        this.deleteData(expenseLastYear);
    }

    /**
     * A jelenlegi hónap bevételeinek összegét adja vissza, a más hónapbelieket
     * és a kiadásokat figyelmen kívül hagyva
     */
    @Test
    void sumAllIncomeForMonth_returnsOnlyCurrentMonthIncome() {
        Mockito.when(currentUser.getUser()).thenReturn(this.user);

        Transaction incomeThisMonth = this.persistSimpleTransaction("incomeThisMonth", TransactionTypeEnum.INCOME,
                new BigDecimal(250), LocalDate.now());
        Transaction expenseThisMonth = this.persistSimpleTransaction("expenseThisMonth", TransactionTypeEnum.OUTCOME,
                new BigDecimal(-250), LocalDate.now());
        Transaction incomeLastYear = this.persistSimpleTransaction("incomeLastYear", TransactionTypeEnum.INCOME,
                new BigDecimal(999), LocalDate.now().minusYears(1));

        BigDecimal result = transactionService.sumAllIncomeForMonth();

        assertEquals(new BigDecimal("250.00"), result);

        this.deleteData(incomeThisMonth);
        this.deleteData(expenseThisMonth);
        this.deleteData(incomeLastYear);
    }

    /**
     * Visszaadja a tranzakciót a részleteivel együtt, ha a useré
     */
    @Test
    void getTransactionByIdForActualUser_returnsTransactionWithDetails() {
        Mockito.when(currentUser.getUser()).thenReturn(this.user);

        Transaction transaction = this.persistSimpleTransaction("lookup", TransactionTypeEnum.INCOME,
                new BigDecimal(100), LocalDate.now());

        Transaction result = transactionService.getTransactionByIdForActualUser(transaction.getId());

        assertEquals(transaction.getId(), result.getId());
        assertEquals(1, result.getTransactionDetails().size());

        this.deleteData(transaction);
    }

    /**
     * Hibát dob, ha a tranzakció nem létezik
     */
    @Test
    void getTransactionByIdForActualUser_throwsWhenNotFound() {
        Mockito.when(currentUser.getUser()).thenReturn(this.user);

        assertThrows(EntityNotFoundException.class, () -> {
            transactionService.getTransactionByIdForActualUser(-1L);
        });
    }

    /**
     * Legfeljebb az utolsó 5 tranzakciót adja vissza, id szerint csökkenő
     * sorrendben
     */
    @Test
    void getLastTransactions_returnsAtMostFiveNewestTransactions() {
        Mockito.when(currentUser.getUser()).thenReturn(this.user);

        List<Transaction> created = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
            created.add(this.persistSimpleTransaction("last" + i, TransactionTypeEnum.INCOME, new BigDecimal(10),
                    LocalDate.now()));
        }

        List<Transaction> result = transactionService.getLastTransactions();

        assertEquals(5, result.size());

        created.forEach(this::deleteData);
    }

    /**
     * A history oldalon a szűrésnek megfelelő tranzakciókat adja vissza
     */
    @Test
    void getHistoryPageData_filtersTransactionsByName() {
        Mockito.when(currentUser.getUser()).thenReturn(this.user);

        var now = LocalDate.now();

        Transaction matching = this.persistSimpleTransaction("groceries shopping", TransactionTypeEnum.OUTCOME,
                new BigDecimal(-10), now);
        Transaction nonMatching = this.persistSimpleTransaction("salary", TransactionTypeEnum.INCOME,
                new BigDecimal(1000), now);

        List<Transaction> result = transactionService.getHistoryPageData(new TransactionFilter("groceries", now));

        assertEquals(1, result.size());
        assertEquals(matching.getId(), result.get(0).getId());

        this.deleteData(matching);
        this.deleteData(nonMatching);
    }

    /**
     * Törléskor a tranzakció soft delete-tel törlődik, így a normál
     * lekérdezésekben már nem jelenik meg
     */
    @Test
    void deleteTransaction_softDeletesTransactionForOwner() {
        Mockito.when(currentUser.getUser()).thenReturn(this.user);

        Transaction transaction = this.persistSimpleTransaction("toDelete", TransactionTypeEnum.INCOME,
                new BigDecimal(100), LocalDate.now());

        transactionService.deleteTransaction(transaction.getId());

        assertThrows(EntityNotFoundException.class, () -> {
            transactionService.getTransactionByIdForActualUser(transaction.getId());
        });
        assertEquals(0, transactionRepo.count());

        this.deleteData(transaction);
    }

    /**
     * Segédfüggvény: egyszerű, egy detailos tranzakció létrehozásához és
     * elmentéséhez
     */
    private Transaction persistSimpleTransaction(String name, TransactionTypeEnum type, BigDecimal price,
            LocalDate date) {
        TransactionDetailSaveCommand detail = new TransactionDetailSaveCommand(
                TransactionDetail.DEFAULT_DETAIL_NAME, price, List.of(), type);
        TransactionCreateCommand command = new TransactionCreateCommand(name, null, date, type, List.of(detail),
                List.of());

        return transactionService.createTransaction(command);
    }

    private void deleteData(Transaction transaction) {
        detailCategoryRepo.deleteAllInBatch();
        categoryRepo.deleteAllInBatch();
        transactionDetailRepo.deleteAllInBatch();
        // Tranzakciót csak így lehet törölni, mert softDelete van rajta
        transactionRepo.hardDeleteTransaction(transaction.getId());
    }
}
