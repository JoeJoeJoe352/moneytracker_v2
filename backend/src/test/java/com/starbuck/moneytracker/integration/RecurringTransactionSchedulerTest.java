package com.starbuck.moneytracker.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.springframework.transaction.support.TransactionTemplate;

import com.starbuck.moneytracker.commands.RecurringTransactionSaveCommand;
import com.starbuck.moneytracker.commands.TransactionSaveCommand;
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
import com.starbuck.moneytracker.mapper.TransactionMapper;
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
    private TransactionRepository transactionrepo;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepo;

    @Autowired
    private RecurringTransactionService service;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private TransactionTemplate transactionTemplate;

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

        scheduler = new RecurringTransactionScheduler(service, fixedClock);
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

        scheduler = new RecurringTransactionScheduler(service, fixedClock);

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

    @Test
    void testWeeklyRecurringTransactionMovesOneWeek() {
        // GIVEN
        createRecurringTransaction(
                "Edzés",
                LocalDate.now(fixedClock),
                RecurrenceIntervalEnum.WEEKLY,
                TransactionTypeEnum.OUTCOME,
                new BigDecimal("-5000"),
                List.of());

        // WHEN
        scheduler.generateRecurrentTransactions();

        // THEN
        assertEquals(1, transactionrepo.count());
        assertEquals(
                LocalDate.of(2026, 11, 7),
                recurringTransactionRepo.findAll().getFirst().getNextOccurrenceDate());
    }

    @Test
    void testIncomeRecurringTransactionKeepsType() {
        // GIVEN
        createRecurringTransaction(
                "Fizetés",
                LocalDate.now(fixedClock),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.INCOME,
                new BigDecimal("500000"),
                List.of());

        // WHEN
        scheduler.generateRecurrentTransactions();

        // THEN
        Transaction transaction = transactionrepo.findAll().getFirst();
        assertEquals(TransactionTypeEnum.INCOME, transaction.getTransactionType());
        assertEquals(0, new BigDecimal("500000").compareTo(transaction.getPriceSum()));
    }

    @Test
    void testNotDueRecurringTransactionIsSkipped() {
        // GIVEN
        // holnap esedékes
        createRecurringTransaction(
                "Spotify",
                LocalDate.of(2026, 11, 1),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                BigDecimal.TEN.negate(),
                List.of());

        // WHEN
        scheduler.generateRecurrentTransactions();

        // THEN
        assertEquals(0, transactionrepo.count());
        assertEquals(
                LocalDate.of(2026, 11, 1),
                recurringTransactionRepo.findAll().getFirst().getNextOccurrenceDate());
    }

    @Test
    void testMissedRecurringTransactionIsGeneratedLater() {
        // GIVEN
        // tegnap lett volna esedékes, de nem futott le a scheduler
        createRecurringTransaction(
                "Spotify",
                LocalDate.of(2026, 10, 30),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                BigDecimal.TEN.negate(),
                List.of());

        // WHEN
        scheduler.generateRecurrentTransactions();

        // THEN
        List<Transaction> transactions = transactionrepo.findAll();
        assertEquals(1, transactions.size());
        // az esedékesség napjára jön létre, nem a futás napjára
        assertEquals(LocalDate.of(2026, 10, 30), transactions.getFirst().getTransactionDate());
        // a kezdő nap (30.) alapján számol tovább
        assertEquals(
                LocalDate.of(2026, 11, 30),
                recurringTransactionRepo.findAll().getFirst().getNextOccurrenceDate());
    }

    @Test
    void testMissedWeeklyRecurringTransactionKeepsItsWeekday() {
        // GIVEN
        // pénteken lett volna esedékes, a scheduler csak szombaton fut le
        createRecurringTransaction(
                "Edzés",
                LocalDate.of(2026, 10, 30),
                RecurrenceIntervalEnum.WEEKLY,
                TransactionTypeEnum.OUTCOME,
                new BigDecimal("-5000"),
                List.of());

        // WHEN
        scheduler.generateRecurrentTransactions();

        // THEN
        assertEquals(
                LocalDate.of(2026, 10, 30), transactionrepo.findAll().getFirst().getTransactionDate());
        // a következő is pénteken lesz, nem szombaton
        assertEquals(
                LocalDate.of(2026, 11, 6),
                recurringTransactionRepo.findAll().getFirst().getNextOccurrenceDate());
    }

    /**
     * Hónapon belül korábbi kezdés: egy futás az összes lemaradt heti előfordulást létrehozza,
     * mindegyiket a saját napjára
     */
    @Test
    void testMissedWeeklyOccurrencesAreAllGeneratedInOneRun() {
        // GIVEN
        createRecurringTransaction(
                "Heti bevásárlás",
                LocalDate.of(2026, 10, 3),
                RecurrenceIntervalEnum.WEEKLY,
                TransactionTypeEnum.OUTCOME,
                new BigDecimal("-20000"),
                List.of());

        // WHEN
        scheduler.generateRecurrentTransactions();

        // THEN: a futás napja (10-31) is benne van
        assertEquals(
                List.of(
                        LocalDate.of(2026, 10, 3),
                        LocalDate.of(2026, 10, 10),
                        LocalDate.of(2026, 10, 17),
                        LocalDate.of(2026, 10, 24),
                        LocalDate.of(2026, 10, 31)),
                this.transactionDates());
        assertEquals(
                LocalDate.of(2026, 11, 7),
                recurringTransactionRepo.findAll().getFirst().getNextOccurrenceDate());
    }

    /**
     * Hosszabb kimaradás után a havi előfordulások a hónap végét is helyesen kezelik
     * (31-i kezdés: 30-i hónapokban 30-án, utána újra 31-én)
     */
    @Test
    void testMissedMonthlyOccurrencesKeepTheStartDayAfterShortMonths() {
        // GIVEN
        createRecurringTransaction(
                "Albérlet",
                LocalDate.of(2026, 5, 31),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                new BigDecimal("-150000"),
                List.of());

        // WHEN
        scheduler.generateRecurrentTransactions();

        // THEN
        assertEquals(
                List.of(
                        LocalDate.of(2026, 5, 31),
                        LocalDate.of(2026, 6, 30),
                        LocalDate.of(2026, 7, 31),
                        LocalDate.of(2026, 8, 31),
                        LocalDate.of(2026, 9, 30),
                        LocalDate.of(2026, 10, 31)),
                this.transactionDates());
        assertEquals(
                LocalDate.of(2026, 11, 30),
                recurringTransactionRepo.findAll().getFirst().getNextOccurrenceDate());
    }

    @Test
    void testOnlyDueRecurringTransactionsAreGenerated() {
        // GIVEN
        createRecurringTransaction(
                "Spotify",
                LocalDate.now(fixedClock),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                BigDecimal.TEN.negate(),
                List.of());
        createRecurringTransaction(
                "Netflix",
                LocalDate.now(fixedClock),
                RecurrenceIntervalEnum.WEEKLY,
                TransactionTypeEnum.OUTCOME,
                BigDecimal.ONE.negate(),
                List.of());
        var notDue = createRecurringTransaction(
                "Edzés",
                LocalDate.of(2026, 11, 1),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                new BigDecimal("-5000"),
                List.of());

        // WHEN
        scheduler.generateRecurrentTransactions();

        // THEN
        Set<String> names =
                transactionrepo.findAll().stream().map(Transaction::getName).collect(Collectors.toSet());
        assertEquals(Set.of("Spotify", "Netflix"), names);

        assertEquals(
                LocalDate.of(2026, 11, 1),
                recurringTransactionRepo.findById(notDue.getId()).orElseThrow().getNextOccurrenceDate());
    }

    @Test
    void testRunningTwiceOnTheSameDayDoesNotDuplicate() {
        // GIVEN
        createRecurringTransaction(
                "Spotify",
                LocalDate.now(fixedClock),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                BigDecimal.TEN.negate(),
                List.of());

        // WHEN
        // a cron naponta többször fut, a második futás már a következő esedékességet
        // látja
        scheduler.generateRecurrentTransactions();
        scheduler.generateRecurrentTransactions();

        // THEN
        assertEquals(1, transactionrepo.count());
        assertEquals(
                LocalDate.of(2026, 11, 30),
                recurringTransactionRepo.findAll().getFirst().getNextOccurrenceDate());
    }

    @Test
    void testStoppedRecurringTransactionIsSkipped() {
        // GIVEN
        var recurringTransaction = createRecurringTransaction(
                "Spotify",
                LocalDate.now(fixedClock),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                BigDecimal.TEN.negate(),
                List.of());
        service.stopRecurringTransaction(recurringTransaction.getId());

        // WHEN
        scheduler.generateRecurrentTransactions();

        // THEN
        assertEquals(0, transactionrepo.count());
        assertEquals(
                LocalDate.now(fixedClock),
                recurringTransactionRepo.findAll().getFirst().getNextOccurrenceDate());
    }

    @Test
    void testDeletedRecurringTransactionIsSkipped() {
        // GIVEN
        var recurringTransaction = createRecurringTransaction(
                "Spotify",
                LocalDate.now(fixedClock),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                BigDecimal.TEN.negate(),
                List.of());
        service.deleteRecurringTransaction(recurringTransaction.getId());

        // WHEN
        scheduler.generateRecurrentTransactions();

        // THEN
        assertEquals(0, transactionrepo.count());
    }

    @Test
    void testMultipleCategoriesAreLinkedToTheDetail() {
        // GIVEN
        Category category1 = categoryRepo.save(new Category("előfizetés", this.user, LangEnum.HU));
        Category category2 = categoryRepo.save(new Category("szórakozás", this.user, LangEnum.HU));
        createRecurringTransaction(
                "Spotify",
                LocalDate.now(fixedClock),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                BigDecimal.TEN.negate(),
                List.of(category1.getId(), category2.getId()));

        // WHEN
        scheduler.generateRecurrentTransactions();

        // THEN
        assertEquals(1, transactionDetailRepo.count());
        Set<Long> categoryIds = detailCategoryRepo.findAll().stream()
                .map(link -> link.getCategory().getId())
                .collect(Collectors.toSet());
        assertEquals(Set.of(category1.getId(), category2.getId()), categoryIds);
    }

    /**
     * Élesben a scheduler a saját szálán fut, request nélkül, így nincs
     * bejelentkezett user a SecurityContextben
     */
    @Test
    void testSchedulerWorksWithoutLoggedInUser() {
        // GIVEN
        Category category = categoryRepo.save(new Category("előfizetés", this.user, LangEnum.HU));
        var recurringTransaction = createRecurringTransaction(
                "Spotify",
                LocalDate.now(fixedClock),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                BigDecimal.TEN.negate(),
                List.of(category.getId()));
        // nincs bejelentkezett user
        Mockito.when(currentUser.getUser()).thenReturn(null);

        // WHEN
        scheduler.generateRecurrentTransactions();

        // THEN
        List<Transaction> transactions = transactionrepo.findAll();
        assertEquals(1, transactions.size());
        assertEquals(wallet.getId(), transactions.getFirst().getWallet().getId());
        assertEquals(
                recurringTransaction.getId(),
                transactions.getFirst().getRecurringTransaction().getId());
        assertEquals(
                category.getId(),
                detailCategoryRepo.findAll().getFirst().getCategory().getId());
    }

    @Test
    void testFailingRecurringTransactionDoesNotBlockOthers() {
        // GIVEN
        // a hibás van elöl a listában (kisebb id, ugyanaz a nap)
        var broken = createRecurringTransaction(
                "Hibás",
                LocalDate.now(fixedClock),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                BigDecimal.TEN.negate(),
                List.of());
        var valid = createRecurringTransaction(
                "Spotify",
                LocalDate.now(fixedClock),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                BigDecimal.TEN.negate(),
                List.of());
        // rossz adat a db-ben: kiadás pozitív összeggel, a tranzakció parancs
        // validációja eldobja
        jdbcTemplate.update("UPDATE recurring_transaction SET price_sum = 10 WHERE id = ?", broken.getId());

        // WHEN
        scheduler.generateRecurrentTransactions();

        // THEN
        List<Transaction> transactions = transactionrepo.findAll();
        assertEquals(1, transactions.size());
        assertEquals("Spotify", transactions.getFirst().getName());

        // a hibás nem lépett tovább, a következő futás újra megpróbálja
        assertEquals(
                LocalDate.now(fixedClock),
                recurringTransactionRepo.findById(broken.getId()).orElseThrow().getNextOccurrenceDate());
        assertEquals(
                LocalDate.of(2026, 11, 30),
                recurringTransactionRepo.findById(valid.getId()).orElseThrow().getNextOccurrenceDate());
    }

    /**
     * A scheduler listázása és a generálás között a user leállítja az ismétlődő
     * tranzakciót. A generálás a friss állapotot nézi, nem a listázáskorit, és nem
     * írja vissza a régi státuszt
     */
    @Test
    void testRecurringTransactionStoppedAfterListingIsSkipped() {
        // GIVEN
        var recurringTransaction = createRecurringTransaction(
                "Spotify",
                LocalDate.now(fixedClock),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                BigDecimal.TEN.negate(),
                List.of());
        List<Long> listedIds = service.listExpiredRecurringTransactionIds(LocalDate.now(fixedClock));
        assertEquals(List.of(recurringTransaction.getId()), listedIds);

        service.stopRecurringTransaction(recurringTransaction.getId());

        // WHEN
        service.generateTransactionFromTemplate(listedIds.getFirst(), LocalDate.now(fixedClock));

        // THEN
        assertEquals(0, transactionrepo.count());
        RecurringTransaction fromDb =
                recurringTransactionRepo.findById(recurringTransaction.getId()).orElseThrow();
        assertEquals(RecurringTransactionStatusEnum.STOPPED, fromDb.getStatus());
        assertEquals(LocalDate.now(fixedClock), fromDb.getNextOccurrenceDate());
    }

    /**
     * Ha két futás (pl. két szerver példány) ugyanazt az id-t listázta, a második
     * már nem esedékesnek látja, így nem jön létre duplikált tranzakció
     */
    @Test
    void testAlreadyGeneratedRecurringTransactionIsNotGeneratedAgain() {
        // GIVEN
        var recurringTransaction = createRecurringTransaction(
                "Spotify",
                LocalDate.now(fixedClock),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                BigDecimal.TEN.negate(),
                List.of());

        // WHEN
        service.generateTransactionFromTemplate(recurringTransaction.getId(), LocalDate.now(fixedClock));
        service.generateTransactionFromTemplate(recurringTransaction.getId(), LocalDate.now(fixedClock));

        // THEN
        assertEquals(1, transactionrepo.count());
        // csak egyszer lépett tovább
        assertEquals(
                LocalDate.of(2026, 11, 30),
                recurringTransactionRepo
                        .findById(recurringTransaction.getId())
                        .orElseThrow()
                        .getNextOccurrenceDate());
    }

    /**
     * A szerkesztő felületnek küldött adatban benne van az ismétlődő tranzakció id-ja, amíg az
     * nincs törölve. Törlés után a tranzakció továbbra is betölthető, de az id már nem jön
     */
    @Test
    void testEditDtoContainsRecurringTransactionIdUntilItIsDeleted() {
        // GIVEN
        var recurringTransaction = createRecurringTransaction(
                "Spotify",
                LocalDate.now(fixedClock),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                BigDecimal.TEN.negate(),
                List.of());
        scheduler.generateRecurrentTransactions();
        long transactionId = transactionrepo.findAll().getFirst().getId();

        // WHEN
        // a controllerhez hasonlóan session-ön belül, hogy a lazy kapcsolatok betölthetők legyenek
        var dtoBeforeDelete = transactionTemplate.execute(status ->
                transactionMapper.toEditDto(transactionService.getTransactionByIdForActualUser(transactionId)));
        service.deleteRecurringTransaction(recurringTransaction.getId());
        var dtoAfterDelete = transactionTemplate.execute(status ->
                transactionMapper.toEditDto(transactionService.getTransactionByIdForActualUser(transactionId)));

        // THEN
        assertEquals(recurringTransaction.getId(), dtoBeforeDelete.recurringTransactionId());
        assertEquals(TransactionSpecialTypeEnum.RECURRING, dtoAfterDelete.specialType());
        assertNull(dtoAfterDelete.recurringTransactionId());
    }

    /**
     * A törölt ismétlődő tranzakciót a Hibernate null-ként tölti be. A tranzakció módosítása ezt a
     * null-t nem írhatja vissza, a kapcsolatnak meg kell maradnia
     */
    @Test
    void testEditingTransactionKeepsLinkToDeletedRecurringTransaction() {
        // GIVEN
        var recurringTransaction = createRecurringTransaction(
                "Spotify",
                LocalDate.now(fixedClock),
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                BigDecimal.TEN.negate(),
                List.of());
        scheduler.generateRecurrentTransactions();
        long transactionId = transactionrepo.findAll().getFirst().getId();
        service.deleteRecurringTransaction(recurringTransaction.getId());

        // WHEN
        transactionService.updateTransaction(
                transactionId,
                new TransactionSaveCommand(
                        "Spotify átnevezve",
                        BigDecimal.TEN.negate(),
                        LocalDate.now(fixedClock),
                        TransactionTypeEnum.OUTCOME,
                        List.of(),
                        List.of(),
                        wallet.getId()));

        // THEN
        // natív sql, mert a törölt ismétlődő tranzakciót a repository nem látja
        assertEquals(
                recurringTransaction.getId(),
                jdbcTemplate.queryForObject(
                        "SELECT recurring_transaction_id FROM transactions WHERE id = ?", Long.class, transactionId));
        assertEquals(
                "Spotify átnevezve",
                transactionrepo.findById(transactionId).orElseThrow().getName());
    }

    /**
     * A létrejött tranzakciók dátumai növekvő sorrendben
     */
    private List<LocalDate> transactionDates() {
        return transactionrepo.findAll().stream()
                .map(Transaction::getTransactionDate)
                .sorted()
                .toList();
    }

    private RecurringTransaction createRecurringTransaction(
            String name,
            LocalDate date,
            RecurrenceIntervalEnum interval,
            TransactionTypeEnum type,
            BigDecimal price,
            List<Long> categoryIds) {
        return service.createRecurringTransaction(new RecurringTransactionSaveCommand(
                name, price, date, interval, type, categoryIds, wallet.getId(), false));
    }
}
