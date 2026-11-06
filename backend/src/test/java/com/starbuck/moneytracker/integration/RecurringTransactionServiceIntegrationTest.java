package com.starbuck.moneytracker.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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

import com.starbuck.moneytracker.commands.RecurringTransactionSaveCommand;
import com.starbuck.moneytracker.commands.UserCreateCommand;
import com.starbuck.moneytracker.dto.RecurringTransactionMonthlySummaryDto;
import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.RecurringTransaction;
import com.starbuck.moneytracker.entity.RecurringTransactionCategory;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enums.CurrencyEnum;
import com.starbuck.moneytracker.entity.enums.LangEnum;
import com.starbuck.moneytracker.entity.enums.RecurrenceIntervalEnum;
import com.starbuck.moneytracker.entity.enums.RecurringTransactionStatusEnum;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;
import com.starbuck.moneytracker.entity.enums.WalletTypeEnum;
import com.starbuck.moneytracker.exception.EntityNotFoundException;
import com.starbuck.moneytracker.repository.CategoryRepository;
import com.starbuck.moneytracker.repository.RecurringTransactionCategoryRepository;
import com.starbuck.moneytracker.repository.RecurringTransactionRepository;
import com.starbuck.moneytracker.repository.TransactionDetailCategoryRepository;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.UserRepository;
import com.starbuck.moneytracker.repository.WalletRepository;
import com.starbuck.moneytracker.service.RecurringTransactionService;
import com.starbuck.moneytracker.service.UserService;
import com.starbuck.moneytracker.testsupport.MySqlContainerTest;
import com.starbuck.moneytracker.util.CurrentUserUtil;

@SpringBootTest
// csak így használható a beforeall, mert egyébként statikusan futna és nem
// elérhető az injektált dolgok
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecurringTransactionServiceIntegrationTest extends MySqlContainerTest {

    private static final LocalDate START_DATE = LocalDate.of(2026, 5, 31);

    /** A havi összegzés tesztjeinek hónapja */
    private static final YearMonth FEBRUARY = YearMonth.of(2026, 2);

    private static final LocalDate FEB_5 = FEBRUARY.atDay(5);

    @Autowired
    private RecurringTransactionService recurringTransactionService;

    @Autowired
    private RecurringTransactionRepository recurringTransactionRepo;

    @Autowired
    private RecurringTransactionCategoryRepository recurringTransactionCategoryRepo;

    @Autowired
    private CategoryRepository categoryRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private UserService userService;

    @Autowired
    private WalletRepository walletRepo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionDetailRepository transactionDetailRepo;

    @Autowired
    private TransactionDetailCategoryRepository detailCategoryRepo;

    @MockitoBean
    private CurrentUserUtil currentUser;

    private User user;
    private Wallet wallet;
    // Teszten belül létrehozott másik user, a cleanUp törli
    private User anotherUser;

    @BeforeAll
    void beforeAll() {
        UserCreateCommand command = new UserCreateCommand("recurringuser", "recurring@email.com", "password");
        this.user = userService.createUser(command);
        this.wallet = walletRepo.findByUserId(this.user.getId()).get(0);
    }

    /**
     * @MockitoBean mockokat Spring minden teszt metódus után resetel, ezért
     *              a stubbolást minden teszt előtt újra be kell állítani
     */
    @BeforeEach
    void mockUserUtil() {
        Mockito.when(currentUser.getUser()).thenReturn(this.user);
    }

    /**
     * Minden teszt után töröl mindent, amit a teszt létrehozott (a közös user és
     * wallet kivételével), így egy elbukó assertion sem hagy maga után adatot
     */
    @AfterEach
    void cleanUp() {
        // a summarizeRecurrentTransactions tesztek által létrehozott tranzakciók
        detailCategoryRepo.deleteAllInBatch();
        transactionDetailRepo.deleteAllInBatch();
        // Natív sql, mert a soft delete miatt a repository a törölt tranzakciókat nem látja
        jdbcTemplate.update(
                "DELETE FROM transactions WHERE wallet_id IN (SELECT id FROM wallet WHERE user_id = ?)",
                this.user.getId());
        if (this.anotherUser != null) {
            jdbcTemplate.update(
                    "DELETE FROM transactions WHERE wallet_id IN (SELECT id FROM wallet WHERE user_id = ?)",
                    this.anotherUser.getId());
        }
        recurringTransactionCategoryRepo.deleteAllInBatch();
        // Natív sql, mert a soft delete miatt a repository a törölt ismétlődő
        // tranzakciókat nem látja
        jdbcTemplate.update("DELETE FROM recurring_transaction");
        // Az előre definiált (user nélküli) kategóriák megmaradnak
        jdbcTemplate.update("DELETE FROM category WHERE user_id IS NOT NULL");
        jdbcTemplate.update("DELETE FROM wallet WHERE user_id = ? AND id <> ?", this.user.getId(), this.wallet.getId());
        if (this.anotherUser != null) {
            jdbcTemplate.update("DELETE FROM wallet WHERE user_id = ?", this.anotherUser.getId());
            userRepo.delete(this.anotherUser);
            this.anotherUser = null;
        }
    }

    @AfterAll
    void afterAll() {
        walletRepo.delete(this.wallet);
        userRepo.delete(this.user);
    }

    @Test
    void createRecurringTransaction_persistsAllEntities() {
        // GIVEN
        Category category1 = categoryRepo.save(new Category("rezsi", this.user, LangEnum.HU));
        Category category2 = categoryRepo.save(new Category("lakás", this.user, LangEnum.HU));

        RecurringTransactionSaveCommand command = this.createCommand(
                "Albérlet",
                new BigDecimal("-150000.00"),
                START_DATE,
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                List.of(category1.getId(), category2.getId()),
                this.wallet.getId(),
                false);

        // WHEN
        RecurringTransaction saved = recurringTransactionService.createRecurringTransaction(command);

        // THEN
        assertNotNull(saved.getId());

        RecurringTransaction fromDb =
                recurringTransactionRepo.findById(saved.getId()).orElseThrow();
        assertEquals("Albérlet", fromDb.getName());
        assertEquals(new BigDecimal("-150000.00"), fromDb.getPriceSum());
        assertEquals(START_DATE, fromDb.getStartDate());
        assertEquals(START_DATE, fromDb.getNextOccurrenceDate());
        assertEquals(RecurrenceIntervalEnum.MONTHLY, fromDb.getRecurrenceInterval());
        assertEquals(TransactionTypeEnum.OUTCOME, fromDb.getTransactionType());
        assertEquals(RecurringTransactionStatusEnum.ACTIVE, fromDb.getStatus());
        assertEquals(this.wallet.getId(), fromDb.getWallet().getId());

        assertEquals(Set.of(category1.getId(), category2.getId()), this.getLinkedCategoryIds(saved.getId()));
    }

    @Test
    void createRecurringTransaction_withoutCategories_createsNoLinks() {
        // GIVEN
        RecurringTransactionSaveCommand command = this.createCommand(
                "Fizetés",
                new BigDecimal("500000.00"),
                START_DATE,
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.INCOME,
                List.of(),
                this.wallet.getId(),
                false);

        // WHEN
        RecurringTransaction saved = recurringTransactionService.createRecurringTransaction(command);

        // THEN
        assertEquals(1, recurringTransactionRepo.count());
        assertEquals(0, recurringTransactionCategoryRepo.count());
        assertEquals(
                TransactionTypeEnum.INCOME,
                recurringTransactionRepo.findById(saved.getId()).orElseThrow().getTransactionType());
    }

    @Test
    void createRecurringTransaction_withPredefinedCategory_success() {
        // GIVEN
        // Előre definiált (user nélküli) kategória, ezt a cleanUp nem törli, ezért itt kell
        Category predefined = categoryRepo.save(new Category("közös", null, LangEnum.HU));

        try {
            RecurringTransactionSaveCommand command = this.createCommand(
                    "Netflix",
                    new BigDecimal("-4000.00"),
                    START_DATE,
                    RecurrenceIntervalEnum.MONTHLY,
                    TransactionTypeEnum.OUTCOME,
                    List.of(predefined.getId()),
                    this.wallet.getId(),
                    false);

            // WHEN
            RecurringTransaction saved = recurringTransactionService.createRecurringTransaction(command);

            // THEN
            assertEquals(Set.of(predefined.getId()), this.getLinkedCategoryIds(saved.getId()));
        } finally {
            recurringTransactionCategoryRepo.deleteAllInBatch();
            categoryRepo.delete(predefined);
        }
    }

    @Test
    void createRecurringTransaction_withAnotherUsersCategory_throwsAndRollsBack() {
        // GIVEN
        this.anotherUser = userService.createUser(new UserCreateCommand("anotheruser", "another@user.com", "password"));
        Category foreignCategory = categoryRepo.save(new Category("idegen", this.anotherUser, LangEnum.HU));

        RecurringTransactionSaveCommand command = this.createCommand(
                "Albérlet",
                new BigDecimal("-150000.00"),
                START_DATE,
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                List.of(foreignCategory.getId()),
                this.wallet.getId(),
                false);

        // WHEN
        assertThrows(
                IllegalArgumentException.class, () -> recurringTransactionService.createRecurringTransaction(command));

        // THEN
        // a @Transactional miatt a már elmentett ismétlődő tranzakció is visszagörgetődik
        assertEquals(0, recurringTransactionRepo.count());
        assertEquals(0, recurringTransactionCategoryRepo.count());
    }

    @Test
    void createRecurringTransaction_withAnotherUsersWallet_throws() {
        // GIVEN
        this.anotherUser = userService.createUser(new UserCreateCommand("anotheruser", "another@user.com", "password"));
        Wallet foreignWallet = walletRepo.findByUserId(this.anotherUser.getId()).get(0);

        RecurringTransactionSaveCommand command = this.createCommand(
                "Albérlet",
                new BigDecimal("-150000.00"),
                START_DATE,
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                List.of(),
                foreignWallet.getId(),
                false);

        // WHEN
        assertThrows(
                EntityNotFoundException.class, () -> recurringTransactionService.createRecurringTransaction(command));

        // THEN
        assertEquals(0, recurringTransactionRepo.count());
    }

    @Test
    void updateRecurringTransaction_updatesAllFieldsAndReplacesCategories() {
        // GIVEN
        Category oldCategory = categoryRepo.save(new Category("régi", this.user, LangEnum.HU));
        Category keptCategory = categoryRepo.save(new Category("megmaradó", this.user, LangEnum.HU));
        Category newCategory = categoryRepo.save(new Category("új", this.user, LangEnum.HU));
        Wallet secondWallet =
                walletRepo.save(new Wallet("második", this.user, CurrencyEnum.HUF, WalletTypeEnum.DEFAULT));

        RecurringTransaction created = this.persistRecurringTransaction(
                "Albérlet",
                new BigDecimal("-150000.00"),
                TransactionTypeEnum.OUTCOME,
                List.of(oldCategory.getId(), keptCategory.getId()));

        LocalDate newDate = LocalDate.of(2026, 6, 15);
        RecurringTransactionSaveCommand updateCommand = this.createCommand(
                "Fizetés",
                new BigDecimal("300000.00"),
                newDate,
                RecurrenceIntervalEnum.WEEKLY,
                TransactionTypeEnum.INCOME,
                List.of(keptCategory.getId(), newCategory.getId()),
                secondWallet.getId(),
                false);

        // WHEN
        recurringTransactionService.updateRecurringTransaction(created.getId(), updateCommand);

        // THEN
        RecurringTransaction fromDb =
                recurringTransactionRepo.findById(created.getId()).orElseThrow();
        assertEquals("Fizetés", fromDb.getName());
        assertEquals(new BigDecimal("300000.00"), fromDb.getPriceSum());
        assertEquals(newDate, fromDb.getStartDate());
        assertEquals(newDate, fromDb.getNextOccurrenceDate());
        assertEquals(RecurrenceIntervalEnum.WEEKLY, fromDb.getRecurrenceInterval());
        assertEquals(TransactionTypeEnum.INCOME, fromDb.getTransactionType());
        assertEquals(RecurringTransactionStatusEnum.ACTIVE, fromDb.getStatus());
        assertEquals(secondWallet.getId(), fromDb.getWallet().getId());

        // a megmaradó kategória újra felvétele nem ütközik a unique key-be
        assertEquals(Set.of(keptCategory.getId(), newCategory.getId()), this.getLinkedCategoryIds(created.getId()));
    }

    @Test
    void updateRecurringTransaction_withEmptyCategories_removesAllLinks() {
        // GIVEN
        Category category = categoryRepo.save(new Category("rezsi", this.user, LangEnum.HU));
        RecurringTransaction created = this.persistRecurringTransaction(
                "Albérlet", new BigDecimal("-150000.00"), TransactionTypeEnum.OUTCOME, List.of(category.getId()));

        RecurringTransactionSaveCommand updateCommand = this.createCommand(
                "Albérlet",
                new BigDecimal("-150000.00"),
                START_DATE,
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                List.of(),
                this.wallet.getId(),
                false);

        // WHEN
        recurringTransactionService.updateRecurringTransaction(created.getId(), updateCommand);

        // THEN
        assertEquals(0, recurringTransactionCategoryRepo.count());
    }

    @Test
    void updateRecurringTransaction_sameDate_keepsStartDate() {
        // GIVEN
        RecurringTransaction created = this.persistRecurringTransaction(
                "Albérlet", new BigDecimal("-150000.00"), TransactionTypeEnum.OUTCOME, List.of());

        // Szimuláljuk, hogy már létrejött egy tranzakció, így a következő esedékesség
        // eltér a kezdő dátumtól
        LocalDate nextOccurrence = LocalDate.of(2026, 6, 30);
        created.setNextOccurrenceDate(nextOccurrence);
        recurringTransactionRepo.save(created);

        RecurringTransactionSaveCommand updateCommand = this.createCommand(
                "Albérlet módosítva",
                new BigDecimal("-160000.00"),
                nextOccurrence,
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                List.of(),
                this.wallet.getId(),
                false);

        // WHEN
        recurringTransactionService.updateRecurringTransaction(created.getId(), updateCommand);

        // THEN
        RecurringTransaction fromDb =
                recurringTransactionRepo.findById(created.getId()).orElseThrow();
        assertEquals(START_DATE, fromDb.getStartDate());
        assertEquals(nextOccurrence, fromDb.getNextOccurrenceDate());
        assertEquals("Albérlet módosítva", fromDb.getName());
        assertEquals(new BigDecimal("-160000.00"), fromDb.getPriceSum());
    }

    @Test
    void updateRecurringTransaction_restartStopped_setsActive() {
        // GIVEN
        RecurringTransaction created = this.persistRecurringTransaction(
                "Albérlet", new BigDecimal("-150000.00"), TransactionTypeEnum.OUTCOME, List.of());
        recurringTransactionService.stopRecurringTransaction(created.getId());

        LocalDate restartDate = LocalDate.of(2026, 9, 1);
        RecurringTransactionSaveCommand restartCommand = this.createCommand(
                "Albérlet",
                new BigDecimal("-150000.00"),
                restartDate,
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                List.of(),
                this.wallet.getId(),
                true);

        // WHEN
        recurringTransactionService.updateRecurringTransaction(created.getId(), restartCommand);

        // THEN
        RecurringTransaction fromDb =
                recurringTransactionRepo.findById(created.getId()).orElseThrow();
        assertEquals(RecurringTransactionStatusEnum.ACTIVE, fromDb.getStatus());
        assertEquals(restartDate, fromDb.getStartDate());
        assertEquals(restartDate, fromDb.getNextOccurrenceDate());
    }

    @Test
    void updateRecurringTransaction_restartActive_throwsAndRollsBack() {
        // GIVEN
        Category category = categoryRepo.save(new Category("rezsi", this.user, LangEnum.HU));
        RecurringTransaction created = this.persistRecurringTransaction(
                "Albérlet", new BigDecimal("-150000.00"), TransactionTypeEnum.OUTCOME, List.of(category.getId()));

        RecurringTransactionSaveCommand restartCommand = this.createCommand(
                "Új név",
                new BigDecimal("-150000.00"),
                START_DATE,
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                List.of(),
                this.wallet.getId(),
                true);

        // WHEN
        assertThrows(
                IllegalArgumentException.class,
                () -> recurringTransactionService.updateRecurringTransaction(created.getId(), restartCommand));

        // THEN
        RecurringTransaction fromDb =
                recurringTransactionRepo.findById(created.getId()).orElseThrow();
        assertEquals("Albérlet", fromDb.getName());
        assertEquals(RecurringTransactionStatusEnum.ACTIVE, fromDb.getStatus());
        assertEquals(Set.of(category.getId()), this.getLinkedCategoryIds(created.getId()));
    }

    @Test
    void updateRecurringTransaction_withAnotherUsersCategory_throwsAndRollsBack() {
        // GIVEN
        Category ownCategory = categoryRepo.save(new Category("saját", this.user, LangEnum.HU));
        RecurringTransaction created = this.persistRecurringTransaction(
                "Albérlet", new BigDecimal("-150000.00"), TransactionTypeEnum.OUTCOME, List.of(ownCategory.getId()));

        this.anotherUser = userService.createUser(new UserCreateCommand("anotheruser", "another@user.com", "password"));
        Category foreignCategory = categoryRepo.save(new Category("idegen", this.anotherUser, LangEnum.HU));

        RecurringTransactionSaveCommand updateCommand = this.createCommand(
                "Új név",
                new BigDecimal("-150000.00"),
                START_DATE,
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                List.of(foreignCategory.getId()),
                this.wallet.getId(),
                false);

        // WHEN
        assertThrows(
                IllegalArgumentException.class,
                () -> recurringTransactionService.updateRecurringTransaction(created.getId(), updateCommand));

        // THEN
        // a régi kategória kapcsolatok törlése is visszagörgetődik
        assertEquals(
                "Albérlet",
                recurringTransactionRepo.findById(created.getId()).orElseThrow().getName());
        assertEquals(Set.of(ownCategory.getId()), this.getLinkedCategoryIds(created.getId()));
    }

    @Test
    void updateRecurringTransaction_ofAnotherUser_throws() {
        // GIVEN
        RecurringTransaction foreign = this.persistForAnotherUser();

        RecurringTransactionSaveCommand updateCommand = this.createCommand(
                "Feltört",
                new BigDecimal("-1.00"),
                START_DATE,
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                List.of(),
                this.wallet.getId(),
                false);

        // WHEN
        assertThrows(
                EntityNotFoundException.class,
                () -> recurringTransactionService.updateRecurringTransaction(foreign.getId(), updateCommand));

        // THEN
        assertEquals(
                "Idegen",
                recurringTransactionRepo.findById(foreign.getId()).orElseThrow().getName());
    }

    @Test
    void listRecurringTransactions_returnsOnlyOwnNotDeletedOrderedByNextOccurrence() {
        // GIVEN
        RecurringTransaction later = this.persistRecurringTransaction(
                "Később", new BigDecimal("-100.00"), TransactionTypeEnum.OUTCOME, List.of(), LocalDate.of(2026, 8, 1));
        RecurringTransaction sooner = this.persistRecurringTransaction(
                "Hamarabb",
                new BigDecimal("-100.00"),
                TransactionTypeEnum.OUTCOME,
                List.of(),
                LocalDate.of(2026, 7, 1));
        RecurringTransaction stopped = this.persistRecurringTransaction(
                "Leállított",
                new BigDecimal("-100.00"),
                TransactionTypeEnum.OUTCOME,
                List.of(),
                LocalDate.of(2026, 9, 1));
        recurringTransactionService.stopRecurringTransaction(stopped.getId());
        RecurringTransaction deleted = this.persistRecurringTransaction(
                "Törölt", new BigDecimal("-100.00"), TransactionTypeEnum.OUTCOME, List.of(), LocalDate.of(2026, 6, 1));
        recurringTransactionService.deleteRecurringTransaction(deleted.getId());
        this.persistForAnotherUser();

        // WHEN
        List<RecurringTransaction> result = recurringTransactionService.listRecurringTransactions();

        // THEN
        // a leállítottak is megjelennek, a törölt és a más userhez tartozó nem
        assertEquals(
                List.of(sooner.getId(), later.getId(), stopped.getId()),
                result.stream().map(RecurringTransaction::getId).toList());
    }

    @Test
    void getRecurringTransactionByIdForActualUser_returnsOwn() {
        // GIVEN
        RecurringTransaction created = this.persistRecurringTransaction(
                "Albérlet", new BigDecimal("-150000.00"), TransactionTypeEnum.OUTCOME, List.of());

        // WHEN
        RecurringTransaction result =
                recurringTransactionService.getRecurringTransactionByIdForActualUser(created.getId());

        // THEN
        assertEquals(created.getId(), result.getId());
        assertEquals("Albérlet", result.getName());
    }

    @Test
    void getRecurringTransactionByIdForActualUser_ofAnotherUser_throws() {
        // GIVEN
        RecurringTransaction foreign = this.persistForAnotherUser();

        // WHEN THEN
        assertThrows(
                EntityNotFoundException.class,
                () -> recurringTransactionService.getRecurringTransactionByIdForActualUser(foreign.getId()));
    }

    @Test
    void getRecurringTransactionByIdForActualUser_notExisting_throws() {
        assertThrows(
                EntityNotFoundException.class,
                () -> recurringTransactionService.getRecurringTransactionByIdForActualUser(Long.MAX_VALUE));
    }

    @Test
    void stopRecurringTransaction_setsStatusStopped() {
        // GIVEN
        RecurringTransaction created = this.persistRecurringTransaction(
                "Albérlet", new BigDecimal("-150000.00"), TransactionTypeEnum.OUTCOME, List.of());

        // WHEN
        recurringTransactionService.stopRecurringTransaction(created.getId());

        // THEN
        RecurringTransaction fromDb =
                recurringTransactionRepo.findById(created.getId()).orElseThrow();
        assertEquals(RecurringTransactionStatusEnum.STOPPED, fromDb.getStatus());
        assertTrue(fromDb.isStopped());
    }

    @Test
    void stopRecurringTransaction_ofAnotherUser_throws() {
        // GIVEN
        RecurringTransaction foreign = this.persistForAnotherUser();

        // WHEN
        assertThrows(
                EntityNotFoundException.class,
                () -> recurringTransactionService.stopRecurringTransaction(foreign.getId()));

        // THEN
        assertEquals(
                RecurringTransactionStatusEnum.ACTIVE,
                recurringTransactionRepo.findById(foreign.getId()).orElseThrow().getStatus());
    }

    @Test
    void deleteRecurringTransaction_softDeletes() {
        // GIVEN
        RecurringTransaction created = this.persistRecurringTransaction(
                "Albérlet", new BigDecimal("-150000.00"), TransactionTypeEnum.OUTCOME, List.of());

        // WHEN
        recurringTransactionService.deleteRecurringTransaction(created.getId());

        // THEN
        // a repository már nem látja (@SQLRestriction)
        assertTrue(recurringTransactionRepo.findById(created.getId()).isEmpty());
        assertThrows(
                EntityNotFoundException.class,
                () -> recurringTransactionService.getRecurringTransactionByIdForActualUser(created.getId()));

        // de a sor megmaradt DELETED státusszal
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM recurring_transaction WHERE id = ?", String.class, created.getId());
        assertEquals(RecurringTransactionStatusEnum.DELETED.name(), status);
    }

    @Test
    void deleteRecurringTransaction_alreadyDeleted_throws() {
        // GIVEN
        RecurringTransaction created = this.persistRecurringTransaction(
                "Albérlet", new BigDecimal("-150000.00"), TransactionTypeEnum.OUTCOME, List.of());
        recurringTransactionService.deleteRecurringTransaction(created.getId());

        // WHEN THEN
        assertThrows(
                EntityNotFoundException.class,
                () -> recurringTransactionService.deleteRecurringTransaction(created.getId()));
    }

    @Test
    void deleteRecurringTransaction_ofAnotherUser_throws() {
        // GIVEN
        RecurringTransaction foreign = this.persistForAnotherUser();

        // WHEN
        assertThrows(
                EntityNotFoundException.class,
                () -> recurringTransactionService.deleteRecurringTransaction(foreign.getId()));

        // THEN
        assertTrue(recurringTransactionRepo.findById(foreign.getId()).isPresent());
    }

    /**
     * Valós adatokkal: a létrejött tranzakciót a schedulerrel azonos
     * generateTransactionFromTemplate hozza létre (ez a következő esedékességet is
     * továbblépteti). Sorrend: findAllForUser, azaz nextOccurrenceDate szerint
     */
    @Test
    void summarizeRecurrentTransactions_combinesCreatedAndPlannedFromDatabase() {
        // GIVEN: a lakbér februári esedékessége már létrejött, a következő márciusi
        RecurringTransaction rent = this.persistRecurringTransaction(
                "Albérlet", new BigDecimal("-150000.00"), TransactionTypeEnum.OUTCOME, List.of(), FEB_5);
        recurringTransactionService.generateTransactionFromTemplate(rent.getId(), FEB_5);
        // a netflix februári esedékessége még tervezett
        RecurringTransaction netflix = this.persistRecurringTransaction(
                "Netflix", new BigDecimal("-4000.00"), TransactionTypeEnum.OUTCOME, List.of(), FEB_5.plusDays(15));

        // WHEN
        List<RecurringTransactionMonthlySummaryDto> result =
                recurringTransactionService.summarizeRecurrentTransactions(FEBRUARY);

        // THEN: a netflix (02-20) a lakbér (03-05) előtt
        assertEquals(List.of(netflix.getId(), rent.getId()), this.toIds(result));

        RecurringTransactionMonthlySummaryDto netflixSummary = result.get(0);
        assertAmount("0", netflixSummary.createdSum());
        assertAmount("-4000", netflixSummary.plannedSum());
        assertEquals(FEB_5.plusDays(15), netflixSummary.nextOccurrenceDate());
        assertEquals(CurrencyEnum.HUF, netflixSummary.currencyCode());

        RecurringTransactionMonthlySummaryDto rentSummary = result.get(1);
        assertAmount("-150000", rentSummary.createdSum());
        assertAmount("0", rentSummary.plannedSum());
        assertEquals(LocalDate.of(2026, 3, 5), rentSummary.nextOccurrenceDate());
    }

    /**
     * Az előző hónapban létrejött tranzakció nem számít bele, a hónapi esedékesség
     * tervezettként jelenik meg
     */
    @Test
    void summarizeRecurrentTransactions_ignoresTransactionsCreatedInOtherMonths() {
        RecurringTransaction rent = this.persistRecurringTransaction(
                "Albérlet", new BigDecimal("-150000.00"), TransactionTypeEnum.OUTCOME, List.of(), FEB_5.minusMonths(1));
        recurringTransactionService.generateTransactionFromTemplate(rent.getId(), FEB_5.minusMonths(1));

        List<RecurringTransactionMonthlySummaryDto> result =
                recurringTransactionService.summarizeRecurrentTransactions(FEBRUARY);

        assertEquals(List.of(rent.getId()), this.toIds(result));
        assertAmount("0", result.get(0).createdSum());
        assertAmount("-150000", result.get(0).plannedSum());
    }

    /**
     * Leállított heti ismétlődő tranzakció: a már létrejött összeg megmarad, a
     * hónap hátralévő esedékességei (02-09, 02-16, 02-23) nem számítanak tervezettnek
     */
    @Test
    void summarizeRecurrentTransactions_stoppedRecurringTransactionHasCreatedButNoPlannedSum() {
        LocalDate monday = LocalDate.of(2026, 2, 2);
        RecurringTransaction weekly = recurringTransactionService.createRecurringTransaction(this.createCommand(
                "Heti bevásárlás",
                new BigDecimal("-20000.00"),
                monday,
                RecurrenceIntervalEnum.WEEKLY,
                TransactionTypeEnum.OUTCOME,
                List.of(),
                this.wallet.getId(),
                false));
        recurringTransactionService.generateTransactionFromTemplate(weekly.getId(), monday);
        recurringTransactionService.stopRecurringTransaction(weekly.getId());

        List<RecurringTransactionMonthlySummaryDto> result =
                recurringTransactionService.summarizeRecurrentTransactions(FEBRUARY);

        assertEquals(List.of(weekly.getId()), this.toIds(result));
        assertAmount("-20000", result.get(0).createdSum());
        assertAmount("0", result.get(0).plannedSum());
        assertNull(result.get(0).nextOccurrenceDate());
    }

    /**
     * A törölt ismétlődő tranzakció és a belőle létrejött tranzakciók sem
     * jelennek meg. Mindkét lekérdezés kiszűri a törölteket, így a domain service
     * hiányzó ismétlődő tranzakció miatti ellenőrzése sem dob
     */
    @Test
    void summarizeRecurrentTransactions_ignoresDeletedRecurringTransactionAndItsTransactions() {
        RecurringTransaction rent = this.persistRecurringTransaction(
                "Albérlet", new BigDecimal("-150000.00"), TransactionTypeEnum.OUTCOME, List.of(), FEB_5);
        recurringTransactionService.generateTransactionFromTemplate(rent.getId(), FEB_5);
        recurringTransactionService.deleteRecurringTransaction(rent.getId());

        List<RecurringTransactionMonthlySummaryDto> result =
                recurringTransactionService.summarizeRecurrentTransactions(FEBRUARY);

        assertTrue(result.isEmpty());
    }

    /**
     * Más user ismétlődő tranzakciói és azok tranzakciói nem jelennek meg
     */
    @Test
    void summarizeRecurrentTransactions_ignoresOtherUsersData() {
        RecurringTransaction foreign = this.persistForAnotherUser();
        recurringTransactionService.generateTransactionFromTemplate(foreign.getId(), START_DATE);

        List<RecurringTransactionMonthlySummaryDto> result =
                recurringTransactionService.summarizeRecurrentTransactions(YearMonth.from(START_DATE));

        assertTrue(result.isEmpty());
    }

    private List<Long> toIds(List<RecurringTransactionMonthlySummaryDto> summaries) {
        return summaries.stream()
                .map(RecurringTransactionMonthlySummaryDto::recurringTransactionId)
                .toList();
    }

    /**
     * BigDecimal összehasonlítás a scale-től függetlenül (pl. -150000 == -150000.00)
     */
    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    private RecurringTransactionSaveCommand createCommand(
            String name,
            BigDecimal priceSum,
            LocalDate date,
            RecurrenceIntervalEnum interval,
            TransactionTypeEnum type,
            List<Long> categoryIds,
            Long walletId,
            boolean isRestartMode) {
        return new RecurringTransactionSaveCommand(
                name, priceSum, date, interval, type, categoryIds, walletId, isRestartMode);
    }

    private RecurringTransaction persistRecurringTransaction(
            String name, BigDecimal priceSum, TransactionTypeEnum type, List<Long> categoryIds) {
        return this.persistRecurringTransaction(name, priceSum, type, categoryIds, START_DATE);
    }

    private RecurringTransaction persistRecurringTransaction(
            String name, BigDecimal priceSum, TransactionTypeEnum type, List<Long> categoryIds, LocalDate date) {
        return recurringTransactionService.createRecurringTransaction(this.createCommand(
                name, priceSum, date, RecurrenceIntervalEnum.MONTHLY, type, categoryIds, this.wallet.getId(), false));
    }

    /**
     * Létrehoz egy másik usert és annak egy ismétlődő tranzakcióját, majd
     * visszaállítja az aktuális usert
     */
    private RecurringTransaction persistForAnotherUser() {
        this.anotherUser = userService.createUser(new UserCreateCommand("anotheruser", "another@user.com", "password"));
        Wallet anotherWallet = walletRepo.findByUserId(this.anotherUser.getId()).get(0);

        Mockito.when(currentUser.getUser()).thenReturn(this.anotherUser);
        RecurringTransaction foreign = recurringTransactionService.createRecurringTransaction(this.createCommand(
                "Idegen",
                new BigDecimal("-100.00"),
                START_DATE,
                RecurrenceIntervalEnum.MONTHLY,
                TransactionTypeEnum.OUTCOME,
                List.of(),
                anotherWallet.getId(),
                false));
        Mockito.when(currentUser.getUser()).thenReturn(this.user);
        return foreign;
    }

    /**
     * A kategória kapcsolatok a repository-n keresztül, mert az entitás
     * categoryLinks listája lazy, tranzakción kívül nem tölthető be
     */
    private Set<Long> getLinkedCategoryIds(long recurringTransactionId) {
        return recurringTransactionCategoryRepo.findAll().stream()
                .filter(link -> link.getRecurringTransaction().getId() == recurringTransactionId)
                .map(RecurringTransactionCategory::getCategory)
                .map(Category::getId)
                .collect(Collectors.toSet());
    }
}
