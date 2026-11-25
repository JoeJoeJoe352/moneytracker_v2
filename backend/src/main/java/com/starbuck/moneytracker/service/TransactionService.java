package com.starbuck.moneytracker.service;

import java.time.LocalDate;
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
import com.starbuck.moneytracker.entity.RecurringTransaction;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.Wallet;
import com.starbuck.moneytracker.entity.enums.TransactionSpecialTypeEnum;
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
    public static final int HISTORY_PAGESIZE = 25;

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
     * Tranzakció létrehozása a bejelentkezett user nevében. A wallet és a
     * kategóriák id-ját ellenőrzi, hogy a userhez tartoznak-e
     */
    @Transactional
    public Transaction createTransaction(TransactionSaveCommand createCommand) {
        if (createCommand.getSpecialType() == TransactionSpecialTypeEnum.TRANSFER) {
            throw new UnsupportedOperationException(
                    "Use createLinkedTransaction() to create transfer type transactions");
        }

        return saveNewTransaction(createCommand, createCommand.getSpecialType());
    }

    /**
     * Párban lévő tranzakciók két oldalának létrehozása.
     *
     * @param srcCommand a forrás oldal parancsa
     * @param dstCommand a cél oldal parancsa
     * @return a két elmentett tranzakció
     */
    protected TransactionPair createTransactionPair(
            TransactionSaveCommand srcCommand, TransactionSaveCommand dstCommand) {
        if (srcCommand.getSpecialType() != TransactionSpecialTypeEnum.TRANSFER
                || dstCommand.getSpecialType() != TransactionSpecialTypeEnum.TRANSFER) {
            throw new IllegalArgumentException("Both sides of a transaction pair must be TRANSFER type");
        }
        Wallet srcWallet = walletService.getWalletById(srcCommand.getWalletId());
        Wallet dstWallet = walletService.getWalletById(dstCommand.getWalletId());

        if (srcWallet.getCurrencyCode() != dstWallet.getCurrencyCode()) {
            throw new IllegalArgumentException("Both sides of a transaction pair must have the same currency");
        }

        Transaction srcTransaction = saveNewTransaction(srcCommand, srcCommand.getSpecialType());
        Transaction dstTransaction = saveNewTransaction(dstCommand, dstCommand.getSpecialType());
        return new TransactionPair(srcTransaction, dstTransaction);
    }

    /**
     * Elmenti a tranzakciót és a detailjait a bejelentkezett user nevében. A
     * wallet és a kategóriák id-ját ellenőrzi, hogy a userhez tartoznak-e
     */
    private Transaction saveNewTransaction(TransactionSaveCommand command, TransactionSpecialTypeEnum specialType) {
        Wallet wallet = walletService.getWalletById(command.getWalletId());

        Transaction savedTransactionModel = this.transactionRepo.save(new Transaction(
                command.getTransactionName(),
                command.getTransactionDate(),
                command.getTransactionType(),
                specialType,
                costCalculator.calculateTransactionCost(command),
                wallet));

        transactionDetailService.saveDetails(savedTransactionModel, command);
        return savedTransactionModel;
    }

    /**
     * Ismétlődő tranzakcióból generál tranzakciót (scheduler). Nincs bejelentkezett
     * user, ezért nem ellenőrzi a jogosultságot: a wallet és a kategóriák az
     * ismétlődő
     * tranzakcióból jönnek, ami már a userhez tartozik
     *
     * @param command              a recurring tranzakció adataiból készült parancs
     * @param recurringTransaction a sablon ismétlődő tranzakció
     */
    @Transactional
    public Transaction createTransactionFromRecurringTransaction(
            TransactionSaveCommand command, RecurringTransaction recurringTransaction) {
        Transaction transaction = new Transaction(
                command.getTransactionName(),
                command.getTransactionDate(),
                command.getTransactionType(),
                command.getSpecialType(),
                costCalculator.calculateTransactionCost(command),
                recurringTransaction.getWallet());

        transaction.setRecurringTransaction(recurringTransaction);
        Transaction savedTransactionModel = this.transactionRepo.save(transaction);

        transactionDetailService.saveDetailsWithoutOwnershipCheck(savedTransactionModel, command);
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

        if (transaction.isTransferTransaction()) {
            throw new IllegalArgumentException("Transfer transactions must be updated together with their other side");
        }

        // A szinkronizációs tranzakció a BalanceSync-en keresztül egy wallethez kötött,
        // áthelyezve rossz wallet egyenlegét korrigálná
        if ((transaction.isSyncTransaction() || transaction.isRecurringTransaction())
                && !Objects.equals(transaction.getWallet().getId(), updateCommand.getWalletId())) {
            throw new IllegalArgumentException("Wallet of sync or recurring transaction " + id + " cannot be changed");
        }

        updateTransaction(transaction, updateCommand, wallet);
    }

    /**
     * Párban lévő (TRANSFER) tranzakciók két oldalának együttes módosítása. Mindkét
     * oldalnak a bejelentkezett useréhez kell tartoznia, és a walletjük nem
     * változhat. A hívónak tranzakción belül kell futnia, hogy a két oldal együtt
     * módosuljon vagy együtt guruljon vissza
     *
     * @param srcId      a forrás oldali tranzakció id-ja
     * @param srcCommand a forrás oldal új adatai
     * @param dstId      a cél oldali tranzakció id-ja
     * @param dstCommand a cél oldal új adatai
     */
    protected void updateTransactionPair(
            long srcId, TransactionSaveCommand srcCommand, long dstId, TransactionSaveCommand dstCommand) {
        if (srcCommand.getSpecialType() != TransactionSpecialTypeEnum.TRANSFER
                || dstCommand.getSpecialType() != TransactionSpecialTypeEnum.TRANSFER) {
            throw new IllegalArgumentException("Both sides of a transaction pair must be TRANSFER type");
        }

        Transaction srcTransaction = getTransferTransactionForUpdate(srcId, srcCommand);
        Transaction dstTransaction = getTransferTransactionForUpdate(dstId, dstCommand);

        updateTransaction(srcTransaction, srcCommand, srcTransaction.getWallet());
        updateTransaction(dstTransaction, dstCommand, dstTransaction.getWallet());
    }

    /**
     * Betölti a pár egyik oldalát módosításhoz, és ellenőrzi, hogy TRANSFER
     * típusú, valamint a walletje nem változik
     */
    private Transaction getTransferTransactionForUpdate(long id, TransactionSaveCommand command) {
        Transaction transaction = this.getTransactionByIdForActualUser(id);

        if (!transaction.isTransferTransaction()) {
            throw new IllegalArgumentException("Transaction " + id + " is not a transfer transaction");
        }
        if (!Objects.equals(transaction.getWallet().getId(), command.getWalletId())) {
            throw new IllegalArgumentException("Wallet of transfer transaction " + id + " cannot be changed");
        }
        return transaction;
    }

    /**
     * A tranzakció adatainak felülírása a parancs alapján, és a detailok cseréje
     */
    private void updateTransaction(Transaction transaction, TransactionSaveCommand command, Wallet wallet) {
        transaction.setName(command.getTransactionName());
        transaction.setTransactionDate(command.getTransactionDate());
        transaction.setTransactionType(command.getTransactionType());
        transaction.setPriceSum(costCalculator.calculateTransactionCost(command));
        transaction.setWallet(wallet);
        transactionRepo.save(transaction);

        // egyszerűbb törölni a detailokat + hozzájuk tartozó kategóriákat, mint
        // kikeresni a meglévőket és frissíteni.
        // TODO legyen majd törlés nélküli update
        transactionDetailService.deleteDetails(transaction);
        transactionDetailService.saveDetails(transaction, command);
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
            filterConditions = Specification.allOf(
                    TransactionSpecifications.hasName(filter.name()),
                    TransactionSpecifications.hasDate(filter.date()),
                    TransactionSpecifications.hasTransactionId(filter.transactionId()),
                    TransactionSpecifications.hasTransactionType(filter.type()));
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
        Transaction transaction = findTransactionForDelete(transactionId);

        if (transaction.isTransferTransaction()) {
            throw new IllegalArgumentException(
                    "Use deleteLinkedTransaction api endpoint to delete transfer type transactions");
        }

        if (transaction.isSyncTransaction()) {
            var balanceSync = balanceSyncRepo
                    .findBySyncTransactionId(transaction.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Transaction " + transactionId + " is a sync transaction, but has no BalanceSync"));

            this.balanceSyncRepo.delete(balanceSync);
        }

        this.transactionRepo.delete(transaction);
    }

    /**
     * Párban lévő (TRANSFER) tranzakciók két oldalának együttes törlése (soft
     * delete). Mindkét oldalnak a bejelentkezett useréhez kell tartoznia, és
     * mindkettőt ellenőrzi, mielőtt bármelyiket törölné. A hívónak tranzakción
     * belül kell futnia
     *
     * @param srcId a forrás oldali tranzakció id-ja
     * @param dstId a cél oldali tranzakció id-ja
     */
    protected void deleteTransactionPair(long srcId, long dstId) {
        Transaction srcTransaction = findTransferTransactionForDelete(srcId);
        Transaction dstTransaction = findTransferTransactionForDelete(dstId);

        this.transactionRepo.delete(srcTransaction);
        this.transactionRepo.delete(dstTransaction);
    }

    private Transaction findTransferTransactionForDelete(long transactionId) {
        Transaction transaction = findTransactionForDelete(transactionId);

        if (!transaction.isTransferTransaction()) {
            throw new IllegalArgumentException("Transaction " + transactionId + " is not a transfer transaction");
        }
        return transaction;
    }

    /**
     * A bejelentkezett user adott id-jú tranzakciója törléshez
     *
     * @throws EntityNotFoundException, ha nincs ilyen, vagy nem a useré
     */
    private Transaction findTransactionForDelete(long transactionId) {
        // Azért nem a getTransactionByIdForActualUser van használva, mert itt
        // feleslegesen töltené be a detailokat
        Long userId = currentUser.getUser().getId();
        return this.transactionRepo
                .findById(transactionId)
                .filter(t -> t.getWallet().getUser().getId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionId));
    }

    /**
     * Adott hónapra összegzi az automatikusan létrehozott tranzakciók összegeit
     *
     * @param from
     * @param to
     * @return
     */
    public List<Transaction> summarizeRecurringTransactionCreatedInThisMonth(LocalDate from, LocalDate to) {
        Long userId = currentUser.getUser().getId();
        return this.transactionRepo.findRecurringGeneratedTransactionsInRange(userId, from, to);
    }
}
