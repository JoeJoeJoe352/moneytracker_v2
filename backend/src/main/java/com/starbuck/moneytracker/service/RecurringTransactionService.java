package com.starbuck.moneytracker.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.starbuck.moneytracker.commands.RecurringTransactionSaveCommand;
import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.dto.RecurringTransactionMonthlySummaryDto;
import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.RecurringTransaction;
import com.starbuck.moneytracker.entity.RecurringTransactionCategory;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enums.RecurringTransactionStatusEnum;
import com.starbuck.moneytracker.exception.EntityNotFoundException;
import com.starbuck.moneytracker.repository.RecurringTransactionCategoryRepository;
import com.starbuck.moneytracker.repository.RecurringTransactionRepository;
import com.starbuck.moneytracker.service.domainservice.RecurringTransactionSummaryDomainService;
import com.starbuck.moneytracker.util.CurrentUserUtil;
import com.starbuck.moneytracker.util.TransactionFactory;

@Service
public class RecurringTransactionService {

    /**
     * Ismétlődő tranzakciónként futásonként legfeljebb ennyi lemaradt tranzakció
     * jön létre,
     * hogy egy hosszú kimaradás se terhelje túl egy futást
     */
    public static final int MAX_CATCH_UP_PER_RUN = 100;

    private final RecurringTransactionRepository recurringTransactionRepo;
    private final RecurringTransactionCategoryRepository recurringTransactionCategoryRepo;
    private final CategoryService categoryService;
    private final WalletService walletService;
    private final CurrentUserUtil currentUser;
    private final TransactionService transactionService;
    private final TransactionFactory transactionFactory;

    private final RecurringTransactionSummaryDomainService summaryCalculator =
            new RecurringTransactionSummaryDomainService();

    public RecurringTransactionService(
            RecurringTransactionRepository recurringTransactionRepo,
            RecurringTransactionCategoryRepository recurringTransactionCategoryRepo,
            CategoryService categoryService,
            WalletService walletService,
            CurrentUserUtil currentUser,
            TransactionService transactionService,
            TransactionFactory transactionFactory) {
        this.recurringTransactionRepo = recurringTransactionRepo;
        this.recurringTransactionCategoryRepo = recurringTransactionCategoryRepo;
        this.categoryService = categoryService;
        this.walletService = walletService;
        this.currentUser = currentUser;
        this.transactionService = transactionService;
        this.transactionFactory = transactionFactory;
    }

    /**
     * Ismétlődő tranzakció létrehozása
     *
     * @param command
     * @return
     */
    @Transactional
    public RecurringTransaction createRecurringTransaction(RecurringTransactionSaveCommand command) {
        Wallet wallet = walletService.getWalletById(command.getWalletId());

        RecurringTransaction recurringTransaction = new RecurringTransaction(
                command.getName(),
                command.getPriceSum(),
                command.getTransactionCreateDate(),
                command.getRecurrenceInterval(),
                command.getTransactionType(),
                wallet);
        RecurringTransaction saved = recurringTransactionRepo.save(recurringTransaction);

        this.saveCategoryLinks(saved, command.getCategoryIds());
        return saved;
    }

    /**
     * Frissíti a user adott id-jú ismétlődő tranzakcióját. Ha a dátum
     * változik, a következő esedékesség és a kezdődátum is az új kezdő dátumra áll
     *
     * @param id
     * @param command
     */
    @Transactional
    public void updateRecurringTransaction(long id, RecurringTransactionSaveCommand command) {
        Wallet wallet = walletService.getWalletById(command.getWalletId());
        RecurringTransaction recurringTransaction = this.getRecurringTransactionByIdForActualUser(id);

        if (command.isRestartMode()) {
            if (!recurringTransaction.isStopped()) {
                throw new IllegalArgumentException(
                        "Tried to restart a deleted or already activated recurring transaction");
            }
            recurringTransaction.setStatus(RecurringTransactionStatusEnum.ACTIVE);
        }

        // Ha megváltozott a küldött dátum, akkor újra beállítjuk a kezdődátumot is
        if (!recurringTransaction.getNextOccurrenceDate().equals(command.getTransactionCreateDate())) {
            recurringTransaction.setDates(command.getTransactionCreateDate());
        }

        recurringTransaction.setName(command.getName());
        recurringTransaction.setPriceSum(command.getPriceSum());
        recurringTransaction.setRecurrenceInterval(command.getRecurrenceInterval());
        recurringTransaction.setTransactionType(command.getTransactionType());
        recurringTransaction.setWallet(wallet);

        recurringTransactionRepo.save(recurringTransaction);

        // egyszerűbb törölni a kategória kapcsolatokat, mint kikeresni a meglévőket.
        // A flush kell, különben a Hibernate a törlés előtt szúrná be az újakat, és
        // ugyanaz a kategória a unique key-be ütközne
        recurringTransactionCategoryRepo.deleteAll(recurringTransaction.getCategoryLinks());
        recurringTransactionCategoryRepo.flush();
        this.saveCategoryLinks(recurringTransaction, command.getCategoryIds());
    }

    /*
     * docker compose -f docker-compose.dev.yml exec backend ./mvnw clean compile
     * docker compose -f docker-compose.dev.yml restart backend
     *
     */

    /**
     * Felveszi az ismétlődő tranzakció mintájára az összes, date-ig esedékes
     * tranzakciót, mindegyiket a saját esedékessége napjára, és továbblépteti a
     * következő esedékességet.
     * <p>
     * Futásonként legfeljebb {@value #MAX_CATCH_UP_PER_RUN} tranzakciót hoz létre,
     * a maradékot a következő futás folytatja.
     *
     * @param recurringTransactionId
     * @param date                   a scheduler futásának napja
     * @return a létrehozott tranzakciók száma
     * @throws IllegalStateException ha a következő esedékesség nem lépne előre
     *                               (végtelen ciklus védelem)
     */
    @Transactional
    public int generateTransactionFromTemplate(long recurringTransactionId, LocalDate date) {
        var found = recurringTransactionRepo.findDueRecurringTransactionByIdForUpdate(recurringTransactionId, date);
        // Ez akkor fordulhat elő, ha a user a listázás után, de e függvény meghívása
        // előtt inaktiválta a recurringTransactiont
        if (found.isEmpty()) {
            return 0;
        }
        RecurringTransaction recurringTransaction = found.get();

        int created = 0;
        while (!recurringTransaction.getNextOccurrenceDate().isAfter(date) && created < MAX_CATCH_UP_PER_RUN) {
            LocalDate occurrence = recurringTransaction.getNextOccurrenceDate();
            TransactionSaveCommand command =
                    transactionFactory.createTransactionCommandFromRecurrentTransactionTemplate(
                            recurringTransaction, occurrence);
            // nincs bejelentkezett user, ezért a jogosultság-ellenőrzés nélküli ágat hívjuk
            transactionService.createTransactionFromRecurringTransaction(command, recurringTransaction);

            this.advanceNextOccurrence(recurringTransaction, occurrence);
            created++;
        }
        recurringTransactionRepo.save(recurringTransaction);
        return created;
    }

    /**
     * Továbblépteti az ismétlődő tranzakció következő esedékességét
     *
     * @throws IllegalStateException ha a következő esedékesség nem az aktuális után
     *                               van
     */
    private void advanceNextOccurrence(RecurringTransaction recurringTransaction, LocalDate occurrence) {
        LocalDate nextOccurrence = recurringTransaction
                .getRecurrenceInterval()
                .calculateNextOccurrence(recurringTransaction.getStartDate(), occurrence);
        if (!nextOccurrence.isAfter(occurrence)) {
            throw new IllegalStateException("Next occurrence (" + nextOccurrence + ") is not after " + occurrence
                    + " for RecurringTransaction " + recurringTransaction.getId());
        }
        recurringTransaction.setNextOccurrenceDate(nextOccurrence);
    }

    /**
     * A user összes ismétlődő tranzakciója
     *
     * @return
     */
    @Transactional(readOnly = true)
    public List<RecurringTransaction> listRecurringTransactions() {
        return recurringTransactionRepo.findAllForUser(currentUser.getUser().getId());
    }

    /**
     * Statisztikát csinál az ismétlődő tranzakciókból az adott hónapra. Betölti a
     * hónapban már létrejött tranzakciókat és a user ismétlődő tranzakcióit, a
     * számolást a RecurringTransactionSummaryDomainService végzi
     *
     * @param yearMonth
     * @return
     */
    @Transactional(readOnly = true)
    public List<RecurringTransactionMonthlySummaryDto> summarizeRecurrentTransactions(YearMonth yearMonth) {
        List<Transaction> createdTransactions = transactionService.summarizeRecurringTransactionCreatedInThisMonth(
                yearMonth.atDay(1), yearMonth.plusMonths(1).atDay(1));
        List<RecurringTransaction> recurringTransactions =
                recurringTransactionRepo.findAllForUser(currentUser.getUser().getId());

        return summaryCalculator.summarizeMonthlyStatistic(yearMonth, createdTransactions, recurringTransactions);
    }

    /**
     * Lekéri a user adott id-jú ismétlődő tranzakcióját
     *
     * @throws EntityNotFoundException, ha nincs találat
     * @param id
     * @return
     */
    public RecurringTransaction getRecurringTransactionByIdForActualUser(long id) {
        return recurringTransactionRepo
                .findByIdForUser(id, currentUser.getUser().getId())
                .orElseThrow(() -> new EntityNotFoundException("Recurring transaction not found: " + id));
    }

    /**
     * Leállítja az ismétlődő tranzakciók létrehozását
     *
     * @param id
     */
    public void stopRecurringTransaction(long id) {
        RecurringTransaction recurringTransaction = this.getRecurringTransactionByIdForActualUser(id);
        recurringTransaction.setStatus(RecurringTransactionStatusEnum.STOPPED);
        this.recurringTransactionRepo.save(recurringTransaction);
    }

    /**
     * Törli az ismétlődő tranzakciót (soft delete). A már létrejött tranzakciók
     * megmaradnak
     *
     * @param id
     */
    @Transactional
    public void deleteRecurringTransaction(long id) {
        RecurringTransaction recurringTransaction = this.getRecurringTransactionByIdForActualUser(id);
        recurringTransactionRepo.delete(recurringTransaction);
    }

    /**
     * Létrehozza a kapcsolatokat az ismétlődő tranzakció és a kategóriái között
     *
     * @param recurringTransaction
     * @param categoryIds
     */
    private void saveCategoryLinks(RecurringTransaction recurringTransaction, List<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return;
        }

        for (Category category : categoryService.getCategoriesForUser(categoryIds)) {
            recurringTransactionCategoryRepo.save(new RecurringTransactionCategory(recurringTransaction, category));
        }
    }

    /**
     * Listázza az adott napig esedékes, aktív ismétlődő tranzakciók id-jait
     *
     * @param date
     * @return
     */
    public List<Long> listExpiredRecurringTransactionIds(LocalDate date) {
        return this.recurringTransactionRepo.findDueTransactionIds(date);
    }
}
