package com.starbuck.moneytracker.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.starbuck.moneytracker.dto.HistoryQueryHelperDto;
import com.starbuck.moneytracker.entity.Category;
import com.starbuck.moneytracker.entity.Transaction;
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
        if (savedTransaction.getTransactionType() == null) {
            throw new IllegalArgumentException("transactiontype not set");
        }

        TransactionTypeEnum transactionType = savedTransaction.getTransactionType();
        boolean hasMultipleDetail = transactionDetails.size() > 1;
        boolean hasSingleDetail = transactionDetails.size() == 1;

        for (TransactionDetail detail : transactionDetails) {
            boolean isWeightAndUnitPriceExistingDifferently = (detail.getWeight() != null
                    ^ detail.getUnitPrice() != null);// ^ => XOR operátor

            if (isWeightAndUnitPriceExistingDifferently) {
                throw new IllegalArgumentException("Weight and unitprice both required, when one of them is set");
            }
            if (hasMultipleDetail && detail.getName() == null) {
                throw new IllegalArgumentException("TransactionDetail name must be provided for multiple details.");
            } else if (hasSingleDetail && detail.getName() == null) {
                detail.setName(TransactionDetail.DEFAULT_DETAIL_NAME);
            }

            detail.setPrice(costCalculator.calculateCost(detail, transactionType));
            transactionType.validateDetailPrice(detail.getPrice());

            detail.setTransaction(savedTransaction);

            var detailAfterSave = this.transactionDetailRepo.save(detail);
            if (detail.getCategoryLinks() != null) {
                this.saveCategoryDetailEntries(detailAfterSave, detail.getCategoryLinks());
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
            List<TransactionDetailCategory> categoryLinkModels) {
        categoryLinkModels.forEach((categoryLinkModel) -> {
            Category categoryDummyObject = new Category();
            categoryDummyObject.setId(categoryLinkModel.getCategory().getId());
            TransactionDetailCategory detailCategoryModel = new TransactionDetailCategory(categoryDummyObject,
                    savedDetail);
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
        return this.summarizeForMonth(TransactionTypeEnum.OUTCOME);
    }

    /**
     * Kiszámolja a tranzakciók alapján, hogy jelenlegi hónapban mennyi bevétele
     * volt
     * 
     * @return BigDecimal
     */
    public BigDecimal sumAllIncomeForMonth() {
        return this.summarizeForMonth(TransactionTypeEnum.INCOME);
    }

    /**
     * Összegzi a user adott típusú tranzakcióit a hónapra
     * 
     * @param type
     * @return
     */
    private BigDecimal summarizeForMonth(TransactionTypeEnum type) {
        Long userId = currentUser.getUser().getId();

        return Optional.ofNullable(
                transactionRepo.summarizeTransactionPricesForMonthAndType(userId, type)).orElse(BigDecimal.ZERO);
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
    public void deleteTransaction(long transactionId) {
        // getTransactionByIdForActualUser itt nem használható, mert feleslegesen tölti be a detailokat és ez törléskor bizonyos esetekben problémát okoz
        Long userId = currentUser.getUser().getId();
        Transaction transaction = this.transactionRepo.findById(transactionId)
                .filter(t -> t.getUser().getId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionId));
        this.transactionRepo.delete(transaction);
    }
}
