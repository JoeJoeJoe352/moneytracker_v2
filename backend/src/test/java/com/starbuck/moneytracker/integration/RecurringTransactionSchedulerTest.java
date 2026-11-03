package com.starbuck.moneytracker.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.starbuck.moneytracker.commands.RecurringTransactionSaveCommand;
import com.starbuck.moneytracker.commands.UserCreateCommand;
import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.RecurringTransaction;
import com.starbuck.moneytracker.entity.RecurringTransactionCategory;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.TransactionDetailCategory;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enums.GeneralStatusEnum;
import com.starbuck.moneytracker.entity.enums.LangEnum;
import com.starbuck.moneytracker.entity.enums.RecurrenceIntervalEnum;
import com.starbuck.moneytracker.entity.enums.RecurringTransactionStatusEnum;
import com.starbuck.moneytracker.entity.enums.TransactionSpecialTypeEnum;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;
import com.starbuck.moneytracker.repository.CategoryRepository;
import com.starbuck.moneytracker.repository.RecurringTransactionCategoryRepository;
import com.starbuck.moneytracker.repository.RecurringTransactionRepository;
import com.starbuck.moneytracker.repository.TransactionDetailCategoryRepository;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.repository.UserRepository;
import com.starbuck.moneytracker.repository.WalletRepository;
import com.starbuck.moneytracker.service.RecurringTransactionScheduler;
import com.starbuck.moneytracker.service.RecurringTransactionService;
import com.starbuck.moneytracker.service.TransactionService;
import com.starbuck.moneytracker.service.UserService;
import com.starbuck.moneytracker.testsupport.MySqlContainerTest;
import com.starbuck.moneytracker.util.CurrentUserUtil;
import com.starbuck.moneytracker.util.TransactionFactory;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecurringTransactionSchedulerTest extends MySqlContainerTest {

    @MockitoBean
    private CurrentUserUtil currentUser;

    @Autowired
    private RecurringTransactionCategoryRepository recurringTransactionCategoryRepo;

    @Autowired
    private WalletRepository walletRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private CategoryRepository categoryRepo;

    @Autowired
    private TransactionDetailCategoryRepository detailCategoryRepo;

    @Autowired
    private TransactionDetailRepository transactionDetailRepo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionFactory transactionFactory;

    @Autowired
    private TransactionRepository transactionrepo;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepo;

    @Autowired
    private RecurringTransactionService service;

    private RecurringTransactionScheduler scheduler;
    private Wallet wallet;
    private User user;
    Clock fixedClock;

    @BeforeAll
    void beforeAll() {
        UserCreateCommand command = new UserCreateCommand("testuser", "teszt@email.com", "password");
        this.user = userService.createUser(command);
        this.wallet = walletRepo.findByUserId(this.user.getId()).get(0);
    }

    @BeforeEach
    void setUp() {
        Mockito.when(currentUser.getUser()).thenReturn(this.user);

        // Minden teszt előtt beállítjuk ezt, mert van olyan teszt, ami felülírja
        fixedClock = Clock.fixed(Instant.parse("2026-10-31T00:00:00Z"), ZoneOffset.UTC);

        scheduler = new RecurringTransactionScheduler(
                transactionService, service, fixedClock, recurringTransactionRepo, transactionFactory);
    }

    /**
     * Minden teszt után törli a létrejött tranzakciókat, részleteiket és az
     * ismétlődő tranzakciókat (a közös user és wallet kivételével)
     */
    @AfterEach
    void cleanUp() {
        detailCategoryRepo.deleteAllInBatch();
        transactionDetailRepo.deleteAllInBatch();
        // Natív sql, mert a soft delete miatt a repository a törölt tranzakciókat nem
        // látja
        jdbcTemplate.update(
                "DELETE FROM transactions WHERE wallet_id IN (SELECT id FROM wallet WHERE user_id = ?)",
                this.user.getId());
        recurringTransactionCategoryRepo.deleteAllInBatch();
        jdbcTemplate.update("DELETE FROM recurring_transaction");
        jdbcTemplate.update("DELETE FROM category WHERE user_id = ?", this.user.getId());
    }

    @AfterAll
    void afterAll() {
        walletRepo.delete(this.wallet);
        userRepo.delete(this.user);
    }

    @Test
    void testAddTransactions() {
        // GIVEN
        Category category = categoryRepo.save(new Category("előfizetés", this.user, LangEnum.HU));

        // Új recurring tranzakció, beégetett napon lesz a következő időszaka
        RecurringTransactionSaveCommand command = new RecurringTransactionSaveCommand(
                "Spotify",
                BigDecimal.TEN.negate(),
                LocalDate.now(fixedClock),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                List.of(category.getId()),
                wallet.getId(),
                false);

        service.createRecurringTransaction(command);

        // WHEN
        scheduler.generateRecurrentTransactions();

        // THEN
        List<Transaction> transactions = transactionrepo.findAll();
        assertEquals(1, transactions.size());
        Transaction transaction = transactions.getFirst();

        List<RecurringTransaction> recurringTransactions = recurringTransactionRepo.findAll();
        assertEquals(1, recurringTransactions.size());
        RecurringTransaction recurringTransaction = recurringTransactions.getFirst();

        // a létrejött tranzakció a recurring tranzakció adatait kapja, a mai dátummal
        assertEquals("Spotify", transaction.getName());
        assertEquals(0, BigDecimal.TEN.negate().compareTo(transaction.getPriceSum()));
        assertEquals(LocalDate.of(2026, 10, 31), transaction.getTransactionDate());
        assertEquals(TransactionTypeEnum.OUTCOME, transaction.getTransactionType());
        assertEquals(GeneralStatusEnum.ACTIVE, transaction.getStatus());
        assertEquals(TransactionSpecialTypeEnum.RECURRING, transaction.getSpecialType());
        assertEquals(
                recurringTransaction.getId(),
                transaction.getRecurringTransaction().getId());
        assertEquals(wallet.getId(), transaction.getWallet().getId());

        // a kategória az alapértelmezett detailhez kerül
        List<TransactionDetail> details = transactionDetailRepo.findAll();
        assertEquals(1, details.size());
        assertEquals(transaction.getId(), details.getFirst().getTransaction().getId());

        List<TransactionDetailCategory> detailCategories = detailCategoryRepo.findAll();
        assertEquals(1, detailCategories.size());
        assertEquals(
                details.getFirst().getId(),
                detailCategories.getFirst().getTransactionDetail().getId());
        assertEquals(category.getId(), detailCategories.getFirst().getCategory().getId());

        // csak a következő esedékesség változik, a többi adat marad
        assertEquals(LocalDate.of(2026, 11, 30), recurringTransaction.getNextOccurrenceDate());
        assertEquals(LocalDate.of(2026, 10, 31), recurringTransaction.getStartDate());
        assertEquals("Spotify", recurringTransaction.getName());
        assertEquals(0, BigDecimal.TEN.negate().compareTo(recurringTransaction.getPriceSum()));
        assertEquals(RecurrenceIntervalEnum.MONTHLY, recurringTransaction.getRecurrenceInterval());
        assertEquals(TransactionTypeEnum.OUTCOME, recurringTransaction.getTransactionType());
        assertEquals(RecurringTransactionStatusEnum.ACTIVE, recurringTransaction.getStatus());
        assertEquals(wallet.getId(), recurringTransaction.getWallet().getId());

        // a recurring tranzakció kategória kapcsolata megmarad
        List<RecurringTransactionCategory> recurringCategories = recurringTransactionCategoryRepo.findAll();
        assertEquals(1, recurringCategories.size());
        assertEquals(
                recurringTransaction.getId(),
                recurringCategories.getFirst().getRecurringTransaction().getId());
        assertEquals(
                category.getId(), recurringCategories.getFirst().getCategory().getId());
    }

    /*
     * Január elején indult az ismétlődő tranzakció, beállt febr 28-ra (utolsó nap
     * februárban), de márciusnak a végére kell ettől függetlenül beállítania a
     * dátumot
     */
    @Test
    public void testRecurringTransactionAtTheEndOfTheMonth() {
        // GIVEN
        fixedClock = Clock.fixed(Instant.parse("2026-02-28T00:00:00Z"), ZoneOffset.UTC);

        scheduler = new RecurringTransactionScheduler(
                transactionService, service, fixedClock, recurringTransactionRepo, transactionFactory);

        RecurringTransactionSaveCommand command = new RecurringTransactionSaveCommand(
                "Spotify",
                BigDecimal.TEN.negate(),
                LocalDate.now(fixedClock),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                List.of(),
                wallet.getId(),
                false);

        var recurringModel = service.createRecurringTransaction(command);
        recurringModel.setStartDate(LocalDate.of(2026, 01, 31));
        this.recurringTransactionRepo.save(recurringModel);

        // WHEN
        scheduler.generateRecurrentTransactions();

        // THEN
        List<Transaction> transactions = transactionrepo.findAll();
        assertEquals(1, transactions.size());
        List<RecurringTransaction> recurringTransactions = recurringTransactionRepo.findAll();
        assertEquals(1, recurringTransactions.size());

        Transaction transaction = transactions.getFirst();
        assertEquals(LocalDate.now(fixedClock), transaction.getTransactionDate());

        RecurringTransaction recurringTransaction = recurringTransactions.getFirst();
        assertEquals(LocalDate.of(2026, 03, 31), recurringTransaction.getNextOccurrenceDate());
        assertEquals(LocalDate.of(2026, 01, 31), recurringTransaction.getStartDate());
    }
}
