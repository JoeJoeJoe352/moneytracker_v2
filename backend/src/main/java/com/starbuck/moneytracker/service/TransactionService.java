package com.starbuck.moneytracker.service;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.dto.TransactionFilterDto;
import com.starbuck.moneytracker.dto.WalletSummaryResponseDto;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enums.TransactionTypeEnum;
import com.starbuck.moneytracker.exception.EntityNotFoundException;
import com.starbuck.moneytracker.repository.BalanceSyncRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.service.domainservice.CostCalculatorDomainService;
import com.starbuck.moneytracker.util.CurrentUserUtil;
import com.starbuck.moneytracker.util.HistoryQueryHelper;
import com.starbuck.moneytracker.util.TransactionSpecifications;

@Service
public class TransactionService {
    private static final int HISTORY_PAGESIZE = 30;

    private final TransactionRepository transactionRepo;
    private final TransactionDetailService transactionDetailService;
    private final CurrentUserUtil currentUser;
    private final WalletService walletService;
    private final BalanceSyncRepository balanceSyncRepo;

    private final CostCalculatorDomainService costCalculator = new CostCalculatorDomainService();

    public TransactionService(
            TransactionRepository transactionRepo,
            TransactionDetailService transactionDetailService,
            CurrentUserUtil currentUser,
            WalletService walletService,
            BalanceSyncRepository balanceSyncRepo) {
        this.transactionRepo = transactionRepo;
        this.transactionDetailService = transactionDetailService;
        this.currentUser = currentUser;
        this.walletService = walletService;
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

        transactionDetailService.saveDetails(savedTransactionModel, createCommand);
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

        // A szinkronizációs tranzakció a BalanceSync-en keresztül egy wallethez kötött,
        // áthelyezve rossz wallet egyenlegét korrigálná
        if (transaction.isSyncTransaction()
                && !Objects.equals(transaction.getWallet().getId(), updateCommand.getWalletId())) {
            throw new IllegalArgumentException("Wallet of sync transaction " + id + " cannot be changed");
        }

        transaction.setName(updateCommand.getTransactionName());
        transaction.setTransactionDate(updateCommand.getTransactionDate());
        transaction.setTransactionType(updateCommand.getTransactionType());
        transaction.setPriceSum(costCalculator.calculateTransactionCost(updateCommand));
        transaction.setWallet(wallet);
        transactionRepo.save(transaction);

        // egyszerűbb törölni a detailokat + hozzájuk tartozó kategóriákat, mint
        // kikeresni a meglévőket és frissíteni.
        transactionDetailService.deleteDetails(transaction);
        transactionDetailService.saveDetails(transaction, updateCommand);
    }

    /**
     * Kiszámolja a tranzakciók alapján, hogy jelenlegi hónapban mennyi kiadás volt
     * Pozitív értékkel tér vissza (értelmetlen az, hogy negatív előjeles kiadás)
     */
    public List<WalletSummaryResponseDto> sumAllExpenseForMonth() {
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
    public List<WalletSummaryResponseDto> sumAllIncomeForMonth() {
        return this.summarizeForMonth(TransactionTypeEnum.INCOME);
    }

    /**
     * Összegzi a user adott típusú tranzakcióit a hónapra
     */
    private List<WalletSummaryResponseDto> summarizeForMonth(TransactionTypeEnum type) {
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
                .getTransactionByIdWithDetails(
                        transactionId, currentUser.getUser().getId())
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionId));
    }

    /**
     * Visszatér az utolsó x darab tranzakció objektummal
     *
     * @return List<Transaction>
     */
    public List<Transaction> getLastTransactions() {
        Sort sort = Sort.by(Transaction::getUpdatedAt).descending();

        return this.getHistory(null, sort, 5, 0).getContent();
    }

    /**
     * Visszatér a tranzakciók oldal adataival. Egy tranzakción belül kéri le az
     * oldalt és a darabszámot, így a kettő egymással konzisztens
     *
     * @param filter
     * @return
     */
    @Transactional(readOnly = true)
    public Page<Transaction> getHistoryPageData(TransactionFilterDto filter, int pageNumber) {
        Sort sort = Sort.by(Transaction::getTransactionDate, Transaction::getId).descending();

        return this.getHistory(filter, sort, HISTORY_PAGESIZE, pageNumber);
    }

    /**
     * Listázza a kapott feltételek alapján a tranzakciókat, adott user számára
     *
     * @param TransactionFilterDto filter
     * @return
     */
    private Page<Transaction> getHistory(TransactionFilterDto filter, Sort sort, int limit, int pageNumber) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at lest 1");
        }

        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber must be at least 0");
        }

        Long userId = currentUser.getUser().getId();
        Specification<Transaction> filterConditions = this.createSpecification(filter);

        HistoryQueryHelper dto = new HistoryQueryHelper(limit, pageNumber, sort, filterConditions);

        return this.transactionRepo.findAllForUser(userId, dto);
    }

    /**
     * Visszaadja a specification objectet
     *
     * @param filter
     * @return
     */
    private Specification<Transaction> createSpecification(TransactionFilterDto filter) {
        Specification<Transaction> filterConditions = null;
        if (filter != null) {
            filterConditions = Specification.where(TransactionSpecifications.hasName(filter.name()))
                    .and(TransactionSpecifications.hasDate(filter.date())
                            .and(TransactionSpecifications.hasTransactionId(filter.transactionId()))
                            .and(TransactionSpecifications.hasTransactionType(filter.type())));
        }
        return filterConditions;
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
        Transaction transaction = this.transactionRepo
                .findById(transactionId)
                .filter(t -> t.getWallet().getUser().getId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionId));

        if (transaction.isSyncTransaction()) {
            var balanceSync = balanceSyncRepo
                    .findBySyncTransactionId(transaction.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Transaction " + transactionId + " is a sync transaction, but has no BalanceSync"));

            this.balanceSyncRepo.delete(balanceSync);

            transaction.setSpecialType(null);
            this.transactionRepo.save(transaction);
        }

        this.transactionRepo.delete(transaction);
    }
}
