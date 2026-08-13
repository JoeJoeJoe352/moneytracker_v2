package com.starbuck.moneytracker.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.repository.CategoryRepository;
import com.starbuck.moneytracker.repository.TransactionDetailCategoryRepository;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.service.domainservice.CostCalculatorDomainService;
import com.starbuck.moneytracker.util.CurrentUserUtil;
import com.starbuck.moneytracker.util.TransactionSpecifications;

import jakarta.persistence.EntityNotFoundException;

import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.TransactionDetailCategory;
import com.starbuck.moneytracker.entity.TransactionFilter;
import com.starbuck.moneytracker.entity.enum_entites.TransactionTypeEnum;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepo;

    @Autowired
    private TransactionDetailRepository transactionDetailRepo;

    @Autowired
    private TransactionDetailCategoryRepository transactionDetailCategoryRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CurrentUserUtil currentUser;

    private final CostCalculatorDomainService costCalculator = new CostCalculatorDomainService();

    /**
     * Tranzakció létrehozása
     * Ha hiba van, magától rollbackel a spring
     */
    @Transactional
    public Transaction createTransaction(Transaction transaction, List<TransactionDetail> transactionDetails) {
        // TODO transactionDetails az legyen a transactionmodelben átadva
        BigDecimal sumOfDetailsPrice = transactionDetails.stream()
                .map((detail) -> costCalculator.calculateCost(detail, transaction.getTransactionType()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        transaction.setPriceSum(sumOfDetailsPrice);
        Transaction savedTransactionModel = this.transactionRepo.save(transaction);
        this.saveDetails(savedTransactionModel, transactionDetails);
        return savedTransactionModel;
    }

    /**
     * Frissíti a user adott id-jú tranzakcióját.
     * Akkor használatos, ha a csak egy tranzakciótétel van
     * 
     * @param id
     * @param updatedTransaction
     */
    @Transactional
    public void updateTransaction(Long id, Transaction updatedTransaction,
            List<TransactionDetail> updatedDetails) {
        // ellenőrzöm, hogy a tranzakció a useré-e (nem fogja megtalálni, hogyha nem)
        Transaction transaction = this.getTransactionByIdForActualUser(id);

        if (updatedDetails.isEmpty()) {
            throw new IllegalStateException("Transaction has no details to update.");
        }

        transaction.setName(updatedTransaction.getName());
        transaction.setTransactionDate(updatedTransaction.getTransactionDate());
        transaction.setTransactionType(updatedTransaction.getTransactionType());

        BigDecimal sumOfDetailsPrice = updatedDetails.stream()
                .map((detail) -> costCalculator.calculateCost(detail, updatedTransaction.getTransactionType()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        transaction.setPriceSum(sumOfDetailsPrice);

        transactionRepo.save(transaction);

        // egyszerűbb törölni a detailokat + hozzájuk tartozó kategóriákat, mint
        // kikeresni a meglévőket és frissíteni.
        // Cascade delete miatt ez törli a detailCategory táblában lévők kapcsolat
        // bejegyzéseket is
        transactionDetailRepo.deleteAll(transaction.getTransactionDetails());
        this.saveDetails(transaction, updatedDetails);
    }

    /**
     * Feltölti és elmenti a tranzakciós részleteket
     * 
     * @param savedTransaction
     * @param transactionDetails
     */
    private void saveDetails(Transaction savedTransaction, List<TransactionDetail> transactionDetails) {
        int countOfDetails = transactionDetails.size();
        for (TransactionDetail detail : transactionDetails) {
            detail.setPrice(costCalculator.calculateCost(detail, savedTransaction.getTransactionType()));

            if (countOfDetails > 1 && detail.getName() == null) {
                throw new IllegalArgumentException("TransactionDetail name must be provided for multiple details.");
            } else if (countOfDetails == 1 && detail.getName() == null) {
                detail.setName(TransactionDetail.DEFAULT_DETAIL_NAME);
            }

            savedTransaction.getTransactionType().validateDetailPrice(detail.getPrice());

            if ((detail.getWeight() != null && detail.getUnitPrice() == null) ||
                    (detail.getWeight() == null && detail.getUnitPrice() != null)) {
                throw new IllegalArgumentException("Weight and unitprice both required, when one of them is set");
            }

            detail.setTransaction(savedTransaction);

            var detailAfterSave = this.transactionDetailRepo.save(detail);
            this.saveCategory(detailAfterSave, detail.getCategoryLinks());
        }
    }

    /**
     * Kapcsolatokat létrehozzuk a detail és a hozzá tartozó kategóriák között
     * 
     * @param savedDetail
     * @param categoryLinkModels
     */
    private void saveCategory(TransactionDetail savedDetail, List<TransactionDetailCategory> categoryLinkModels) {
        categoryLinkModels.forEach((categoryLinkModel) -> {
            Category categoryRef = new Category();
            categoryRef.setId(categoryLinkModel.getCategory().getId());
            TransactionDetailCategory detailCategoryModel = new TransactionDetailCategory(categoryRef, savedDetail);
            transactionDetailCategoryRepository.save(detailCategoryModel);
        });
    }

    /**
     * Kiszámolja a tranzakciók alapján, hogy mennyi a jelenlegi pénze a usernek
     * 
     * @return BigDecimal
     */
    public BigDecimal sumAllMoney() {
        BigDecimal sum = this.transactionRepo.summarizeTotalMoneyForUser(currentUser.getUser().getId());
        return sum == null ? BigDecimal.ZERO : sum;
    }

    /**
     * Kiszámolja a tranzakciók alapján, hogy jelenlegi hónapban mennyi kiadás volt
     * 
     * @return BigDecimal
     */
    public BigDecimal sumAllExpenseForMonth() {
        BigDecimal sum = this.transactionRepo.summarizeTransactionPricesForMonthAndType(currentUser.getUser().getId(),
                TransactionTypeEnum.OUTCOME);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    /**
     * Kiszámolja a tranzakciók alapján, hogy jelenlegi hónapban mennyi bevétele
     * volt
     * 
     * @return BigDecimal
     */
    public BigDecimal sumAllIncomeForMonth() {
        BigDecimal sum = this.transactionRepo.summarizeTransactionPricesForMonthAndType(currentUser.getUser().getId(),
                TransactionTypeEnum.INCOME);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    /**
     * Visszatér az utolsó x darab tranzakció objektummal
     * 
     * @return Transaction[]
     */
    public Transaction[] getLastTransactions() {
        int lastTransactionLimit = 5;
        return this.transactionRepo.getLastTransactionsForUserWithLimit(currentUser.getUser().getId(),
                lastTransactionLimit);
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
     * Listázza valamilyen feltételek alapján a tranzakciókat
     * 
     * @param TransactionFilter filter
     * @return
     */
    public List<Transaction> getHistory(TransactionFilter filter) {
        Long userId = currentUser.getUser().getId();
        var spec = Specification
                .where(TransactionSpecifications.hasName(filter.name()))
                .and(TransactionSpecifications.hasDate(filter.dateString()))
                .and(TransactionSpecifications.hasUserId(userId));

        return this.transactionRepo.findAll(spec);
    }

    /**
     * Törli a tranzakciót (soft delete)
     * JPA-ban szűrve van, hogy törölt-e és olyankor nem adja vissza (entity-ben van
     * beállítva)
     * 
     * @param transactionId
     */
    public void deleteTransaction(long transactionId) {
        // jogosultságvizsgálat is
        Transaction transaction = this.getTransactionByIdForActualUser(transactionId);
        this.transactionRepo.delete(transaction);
    }
}
