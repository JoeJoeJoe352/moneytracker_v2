package com.starbuck.moneytracker.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.starbuck.moneytracker.commands.RecurringTransactionSaveCommand;
import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.RecurringTransaction;
import com.starbuck.moneytracker.entity.RecurringTransactionCategory;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enums.RecurringTransactionStatusEnum;
import com.starbuck.moneytracker.exception.EntityNotFoundException;
import com.starbuck.moneytracker.repository.CategoryRepository;
import com.starbuck.moneytracker.repository.RecurringTransactionCategoryRepository;
import com.starbuck.moneytracker.repository.RecurringTransactionRepository;
import com.starbuck.moneytracker.util.CurrentUserUtil;

@Service
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepo;
    private final RecurringTransactionCategoryRepository recurringTransactionCategoryRepo;
    private final CategoryRepository categoryRepo;
    private final WalletService walletService;
    private final CurrentUserUtil currentUser;

    public RecurringTransactionService(
            RecurringTransactionRepository recurringTransactionRepo,
            RecurringTransactionCategoryRepository recurringTransactionCategoryRepo,
            CategoryRepository categoryRepo,
            WalletService walletService,
            CurrentUserUtil currentUser) {
        this.recurringTransactionRepo = recurringTransactionRepo;
        this.recurringTransactionCategoryRepo = recurringTransactionCategoryRepo;
        this.categoryRepo = categoryRepo;
        this.walletService = walletService;
        this.currentUser = currentUser;
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
                command.getStartDate(),
                command.getRecurrenceInterval(),
                command.getTransactionType(),
                wallet);
        RecurringTransaction saved = recurringTransactionRepo.save(recurringTransaction);

        this.saveCategoryLinks(saved, command.getCategoryIds());
        return saved;
    }

    /**
     * Frissíti a user adott id-jú ismétlődő tranzakcióját. Ha a kezdő dátum
     * változik, a következő esedékesség is az új kezdő dátumra áll
     *
     * @param id
     * @param command
     */
    @Transactional
    public void updateRecurringTransaction(long id, RecurringTransactionSaveCommand command) {
        Wallet wallet = walletService.getWalletById(command.getWalletId());
        RecurringTransaction recurringTransaction = this.getRecurringTransactionByIdForActualUser(id);

        if (!recurringTransaction.getStartDate().equals(command.getStartDate())) {
            recurringTransaction.setNextOccurrenceDate(command.getStartDate());
        }

        recurringTransaction.setName(command.getName());
        recurringTransaction.setPriceSum(command.getPriceSum());
        recurringTransaction.setStartDate(command.getStartDate());
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
     * Megállítja az ismétlődő tranzakció futását
     *
     * @param id
     */
    public void stopRecurringTransaction(long id) {
        RecurringTransaction recurringTransaction = this.getRecurringTransactionByIdForActualUser(id);
        recurringTransaction.setStatus(RecurringTransactionStatusEnum.STOPPED);
        this.recurringTransactionRepo.save(recurringTransaction);
    }

    /**
     * Újraindítja az ismétlődő tranzakció futását
     *
     * @param id
     */
    public void reActivateRecurringTransaction(long id) {
        RecurringTransaction recurringTransaction = this.getRecurringTransactionByIdForActualUser(id);
        recurringTransaction.setStatus(RecurringTransactionStatusEnum.ACTIVE);
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

        var foundCategories =
                categoryRepo.findAllById(categoryIds, currentUser.getUser().getId());
        if (foundCategories.size() != categoryIds.size()) {
            throw new IllegalArgumentException("Input contains a category id that doesn't belong to the user");
        }

        for (Category category : foundCategories) {
            recurringTransactionCategoryRepo.save(new RecurringTransactionCategory(recurringTransaction, category));
        }
    }
}
