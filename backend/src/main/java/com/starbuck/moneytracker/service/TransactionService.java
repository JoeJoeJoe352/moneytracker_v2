package com.starbuck.moneytracker.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.starbuck.moneytracker.commands.TransactionDetailSaveCommand;
import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.dto.HistoryQueryHelperDto;
import com.starbuck.moneytracker.dto.WalletSummaryDto;
import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.TransactionDetailCategory;
import com.starbuck.moneytracker.entity.TransactionFilter;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;
import com.starbuck.moneytracker.repository.BalanceSyncRepository;
import com.starbuck.moneytracker.repository.CategoryRepository;
import com.starbuck.moneytracker.repository.TransactionDetailCategoryRepository;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.service.domainservice.CostCalculatorDomainService;
import com.starbuck.moneytracker.util.CurrentUserUtil;
import com.starbuck.moneytracker.util.TransactionDetailFactory;
import com.starbuck.moneytracker.util.TransactionSpecifications;

import jakarta.persistence.EntityNotFoundException;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepo;
    private final TransactionDetailRepository transactionDetailRepo;
    private final CategoryRepository categoryRepo;
    private final TransactionDetailCategoryRepository transactionDetailCategoryRepository;
    private final CurrentUserUtil currentUser;
    private final WalletService walletService;
    private final TransactionDetailFactory detailFactory;
    private final BalanceSyncRepository balanceSyncRepo;
    
    private final CostCalculatorDomainService costCalculator = new CostCalculatorDomainService();

    public TransactionService(TransactionRepository transactionRepo, TransactionDetailRepository transactionDetailRepo,
            CategoryRepository categoryRepo, TransactionDetailCategoryRepository transactionDetailCategoryRepository,
            CurrentUserUtil currentUser, WalletService walletService, TransactionDetailFactory detailFactory,
            BalanceSyncRepository balanceSyncRepo) {
        this.transactionRepo = transactionRepo;
        this.transactionDetailRepo = transactionDetailRepo;
        this.categoryRepo = categoryRepo;
        this.transactionDetailCategoryRepository = transactionDetailCategoryRepository;
        this.currentUser = currentUser;
        this.walletService = walletService;
        this.detailFactory = detailFactory;
        this.balanceSyncRepo = balanceSyncRepo;
    }

    /**
     * Tranzakció létrehozása
     */
    @Transactional
    public Transaction createTransaction(TransactionSaveCommand createCommand) {
        Wallet wallet = walletService.getWalletById(createCommand.getWalletId());

        Transaction transaction = new Transaction(
                createCommand.getTransactionName(),
                createCommand.getTransactionDate(),
                createCommand.getTransactionType(),
                costCalculator.calculateTransactionCost(createCommand),
                wallet);
        transaction.setSpecialType(createCommand.getSpecialType());
        Transaction savedTransactionModel = this.transactionRepo.save(transaction);

        this.saveDetails(savedTransactionModel, createCommand);
        return savedTransactionModel;
    }

    /**
     * Frissíti a user adott id-jú tranzakcióját.
     * 
     * @param id
     * @param updateCommand
     */
    @Transactional
    public void updateTransaction(Long id, TransactionSaveCommand updateCommand) {
        Wallet wallet = walletService.getWalletById(updateCommand.getWalletId());

        Transaction transaction = this.getTransactionByIdForActualUser(id);
        transaction.setName(updateCommand.getTransactionName());
        transaction.setTransactionDate(updateCommand.getTransactionDate());
        transaction.setTransactionType(updateCommand.getTransactionType());
        transaction.setPriceSum(costCalculator.calculateTransactionCost(updateCommand));
        transaction.setWallet(wallet);
        transactionRepo.save(transaction);

        // egyszerűbb törölni a detailokat + hozzájuk tartozó kategóriákat, mint
        // kikeresni a meglévőket és frissíteni.
        // Cascade delete miatt ez törli a detailCategory táblában lévők kapcsolat
        // bejegyzéseket is TODO ez legyen direktben törlés inkább, ne cascade delete
        transactionDetailRepo.deleteAll(transaction.getTransactionDetails());
        this.saveDetails(transaction, updateCommand);
    }

    /**
     * Feltölti és elmenti a tranzakciós részleteket
     * 
     * @param savedTransaction
     * @param transactionDetails
     */
    private void saveDetails(Transaction savedTransaction, TransactionSaveCommand createCommand) {
        List<TransactionDetailSaveCommand> details = detailFactory.resolveDetailCommands(createCommand,
                savedTransaction);

        for (TransactionDetailSaveCommand detailCommand : details) {
            TransactionDetail detail = new TransactionDetail(
                    detailCommand.getName(),
                    costCalculator.calculateCost(detailCommand, savedTransaction.getTransactionType()),
                    detailCommand.getWeight(),
                    detailCommand.getUnitPrice(),
                    savedTransaction);
            var detailAfterSave = this.transactionDetailRepo.save(detail);

            if (detailCommand.getCategories() != null && !detailCommand.getCategories().isEmpty()) {
                this.saveCategoryDetailEntries(detailAfterSave, detailCommand.getCategories());
            }
        }

    }

    /**
     * Kapcsolatokat létrehozzuk a detail és a hozzá tartozó kategóriák között
     * 
     * @param savedDetail
     * @param categoryLinkModels
     */
    private void saveCategoryDetailEntries(TransactionDetail savedDetail,
            List<Long> categoryIds) {
        var foundCategories = categoryRepo.findAllById(categoryIds, currentUser.getUser().getId());
        boolean isAllCategoryAvailableForUser = foundCategories.size() == categoryIds.size();
        if (!isAllCategoryAvailableForUser) {
            throw new IllegalArgumentException("Input contains a category id that doesn't belong to the user");
        }
        categoryIds.forEach((id) -> {
            Category categoryDummyObject = new Category();
            categoryDummyObject.setId(id);
            TransactionDetailCategory detailCategoryModel = new TransactionDetailCategory(categoryDummyObject,
                    savedDetail);
            transactionDetailCategoryRepository.save(detailCategoryModel);
        });
    }

    /**
     * Kiszámolja a tranzakciók alapján, hogy jelenlegi hónapban mennyi kiadás volt
     * Pozitív értékkel tér vissza (értelmetlen az, hogy negatív előjeles kiadás)
     */
    public List<WalletSummaryDto> sumAllExpenseForMonth() {
        var result = this.summarizeForMonth(TransactionTypeEnum.OUTCOME);
        result.forEach(walletSummary -> {
            walletSummary.setTotal(walletSummary.getTotal().abs());
        });
        return result;
    }

    /**
     * Kiszámolja a tranzakciók alapján, hogy jelenlegi hónapban mennyi bevétele
     * volt
     */
    public List<WalletSummaryDto> sumAllIncomeForMonth() {
        return this.summarizeForMonth(TransactionTypeEnum.INCOME);
    }

    /**
     * Összegzi a user adott típusú tranzakcióit a hónapra
     */
    private List<WalletSummaryDto> summarizeForMonth(TransactionTypeEnum type) {
        Long userId = currentUser.getUser().getId();

        return transactionRepo.summarizeTransactionPricesForMonthAndType(userId, type);
    }

    /**
     * Lekéri az adott id-jú tranzakcióját a usernek
     * 
     * @throws EntityNotFoundException, ha nincs találat
     * @param transactionId
     * @return
     */
    public Transaction getTransactionByIdForActualUser(Long transactionId) {
        return this.transactionRepo
                .getTransactionByIdWithDetails(transactionId, currentUser.getUser().getId())
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionId));
    }

    /**
     * Visszatér az utolsó x darab tranzakció objektummal
     * 
     * @return List<Transaction>
     */
    public List<Transaction> getLastTransactions() {
        return this.getHistory(null, 5);
    }

    /**
     * Visszatér a tranzakciók oldal adataival
     * 
     * @param filter
     * @return
     */
    public List<Transaction> getHistoryPageData(TransactionFilter filter) {
        return this.getHistory(filter, 30);
    }

    /**
     * Listázza a kapott feltételek alapján a tranzakciókat, adott user számára
     * 
     * @param TransactionFilter filter
     * @return
     */
    private List<Transaction> getHistory(TransactionFilter filter, int limit) {
        Long userId = currentUser.getUser().getId();

        Sort sort = Sort.by("createdAt").descending();

        Specification<Transaction> filterConditions = null;
        if (filter != null) {
            filterConditions = Specification
                    .where(TransactionSpecifications.hasName(filter.name()))
                    .and(TransactionSpecifications.hasDate(filter.dateString()));
        }

        HistoryQueryHelperDto dto = new HistoryQueryHelperDto(limit, sort, filterConditions);

        return this.transactionRepo.findAllForUser(userId, dto);
    }

    /**
     * Törli a tranzakciót (soft delete).
     * 
     * @param transactionId
     */
    @Transactional
    public void deleteTransaction(long transactionId) {
        // getTransactionByIdForActualUser itt nem használható, mert itt feleslegesen
        // töltené be a detailokat
        Long userId = currentUser.getUser().getId();
        Transaction transaction = this.transactionRepo.findById(transactionId)
                .filter(t -> t.getWallet().getUser().getId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionId));

        if (transaction.isSyncTransaction()) {
            var balanceSync = balanceSyncRepo.findBySyncTransactionId(transaction.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Transaction " + transactionId + " is a sync transaction, but has no BalanceSync"));

            this.balanceSyncRepo.delete(balanceSync);

            transaction.setSpecialType(null);
            this.transactionRepo.save(transaction);
        }

        this.transactionRepo.delete(transaction);
    }
}
