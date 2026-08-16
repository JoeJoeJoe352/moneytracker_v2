package com.starbuck.moneytracker.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
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

import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.TransactionDetailCategory;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;
import com.starbuck.moneytracker.repository.CategoryRepository;
import com.starbuck.moneytracker.repository.TransactionDetailCategoryRepository;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.repository.UserRepository;
import com.starbuck.moneytracker.service.TransactionService;
import com.starbuck.moneytracker.util.CurrentUserUtil;

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
        Transaction transaction = new Transaction();
        transaction.setName("Test");
        transaction.setTransactionType(TransactionTypeEnum.INCOME);
        transaction.setTransactionDate(LocalDate.of(2026, 6, 8));
        transaction.setUser(this.user);

        TransactionDetail transactionDetail = new TransactionDetail();
        transactionDetail.setPrice(new BigDecimal(100.00));

        Category category = new Category("tesztkategória", this.user);
        categoryRepo.save(category);

        TransactionDetailCategory categoryJunctionEntry = new TransactionDetailCategory(category, transactionDetail);
        transactionDetail.setCategoryLinks(List.of(categoryJunctionEntry));
        List<TransactionDetail> transactionDetails = Arrays.asList(transactionDetail);

        // WHEN
        Transaction saved = transactionService.createTransaction(transaction, transactionDetails);

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

        this.deleteData(transaction);
    }

    @Test
    void updateTransaction_persistsAllEntities() {
        // GIVEN
        Mockito.when(currentUser.getUser()).thenReturn(this.user);

        Transaction transaction = new Transaction();
        transaction.setName("Test");
        transaction.setTransactionType(TransactionTypeEnum.INCOME);
        transaction.setTransactionDate(LocalDate.of(2026, 6, 8));
        transaction.setUser(this.user);

        TransactionDetail transactionDetail = new TransactionDetail();
        transactionDetail.setPrice(new BigDecimal(100.00));

        Category category1 = new Category("tesztKategória", this.user);
        categoryRepo.save(category1);

        TransactionDetailCategory categoryJunctionEntry = new TransactionDetailCategory(category1, transactionDetail);
        transactionDetail.setCategoryLinks(List.of(categoryJunctionEntry));
        List<TransactionDetail> transactionDetails = Arrays.asList(transactionDetail);

        // Van DB-ben elem mostmár
        transactionService.createTransaction(transaction, transactionDetails);

        // Update elemek létrehozása
        Transaction updateTransaction = new Transaction();
        updateTransaction.setName("Update test");
        updateTransaction.setTransactionType(TransactionTypeEnum.OUTCOME);
        updateTransaction.setTransactionDate(LocalDate.of(2026, 7, 8));
        updateTransaction.setUser(this.user);

        Category category2 = new Category("updateKategória", this.user);
        categoryRepo.save(category2);

        // Detail 1 beállításai - 1 kategóriája van
        TransactionDetail updateTransactionDetail1 = new TransactionDetail();
        updateTransactionDetail1.setPrice(new BigDecimal("-200.00"));
        updateTransactionDetail1.setName("update1");
        TransactionDetailCategory updateCategoryJunctionEntry = new TransactionDetailCategory(category2,
                updateTransactionDetail1);
        updateTransactionDetail1.setCategoryLinks(List.of(updateCategoryJunctionEntry));

        // Detail2 beállításai - 2 kategória beállítva
        TransactionDetail updateTransactionDetail2 = new TransactionDetail();
        updateTransactionDetail2.setWeight(new BigDecimal("0.5"));
        updateTransactionDetail2.setUnitPrice(new BigDecimal("600.00"));
        updateTransactionDetail2.setName("update2");
        TransactionDetailCategory updateCategoryJunctionEntry2_1 = new TransactionDetailCategory(category2,
                updateTransactionDetail2);
        TransactionDetailCategory updateCategoryJunctionEntry2_2 = new TransactionDetailCategory(category1,
                updateTransactionDetail2);
        updateTransactionDetail2
                .setCategoryLinks(List.of(updateCategoryJunctionEntry2_1, updateCategoryJunctionEntry2_2));

        List<TransactionDetail> updateTransactionDetails = Arrays.asList(updateTransactionDetail1,
                updateTransactionDetail2);
        // WHEN
        transactionService.updateTransaction(transaction.getId(), updateTransaction, updateTransactionDetails);

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
        Transaction transaction = new Transaction();
        transaction.setName("hibásteszt");
        transaction.setTransactionType(TransactionTypeEnum.INCOME);
        transaction.setTransactionDate(LocalDate.of(2026, 6, 8));
        transaction.setUser(this.user);

        TransactionDetail transactionDetail = new TransactionDetail();
        transactionDetail.setPrice(new BigDecimal(100));
        // emiatt nem fogja tudni elmenteni a detailst és rollback az egész
        transactionDetail.setName("hosszunev0".repeat(26));
        List<TransactionDetail> transactionDetails = Arrays.asList(transactionDetail);

        assertThrows(DataIntegrityViolationException.class, () -> {
            transactionService.createTransaction(transaction, transactionDetails);
        });

        // rollback miatt nincs egy sem a db-ben
        assertEquals(0, transactionRepo.count());
        assertEquals(0, transactionDetailRepo.count());
    }

    private void deleteData(Transaction transaction) {
        detailCategoryRepo.deleteAllInBatch();
        categoryRepo.deleteAllInBatch();
        transactionDetailRepo.deleteAllInBatch();
        // Tranzakciót csak így lehet törölni, mert softDelete van rajta
        transactionRepo.hardDeleteTransaction(transaction.getId());
    }
}
