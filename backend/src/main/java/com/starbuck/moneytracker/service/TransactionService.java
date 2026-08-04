package com.starbuck.moneytracker.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.service.domainservice.CostCalculatorDomainService;
import com.starbuck.moneytracker.util.CurrentUserUtil;
import com.starbuck.moneytracker.util.TransactionSpecifications;

import jakarta.persistence.EntityNotFoundException;

import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.TransactionFilter;
import com.starbuck.moneytracker.entity.TransactionTypeEnum;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepo;

    @Autowired
    private TransactionDetailRepository transactionDetailRepo;

    @Autowired
    private CurrentUserUtil currentUser;

    private final CostCalculatorDomainService costCalculator = new CostCalculatorDomainService();

    /**
     * Tranzakció létrehozása
     * Ha hiba van, magától rollbackel a spring
     */
    @Transactional
    public Transaction createTransaction(Transaction transaction, List<TransactionDetail> transactionDetails) {
        BigDecimal sumOfDetailsPrice = transactionDetails.stream()
                .map((detail) -> costCalculator.calculateCost(detail, transaction.isOutcome()))
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
        Transaction transaction = this.getTransactionById(id);

        if (updatedDetails.isEmpty()) {
            throw new IllegalStateException("Transaction has no details to update.");
        }

        transaction.setName(updatedTransaction.getName());
        transaction.setTransactionDate(updatedTransaction.getTransactionDate());
        transaction.setTransactionType(updatedTransaction.getTransactionType());

        BigDecimal sumOfDetailsPrice = updatedDetails.stream()
                .map((detail) -> costCalculator.calculateCost(detail, updatedTransaction.isOutcome()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        transaction.setPriceSum(sumOfDetailsPrice);

        transactionRepo.save(transaction);
        // egyszerűbb törölni a detailokat, mint kikeresni a meglévőket és frissíteni
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
            detail.setPrice(costCalculator.calculateCost(detail, savedTransaction.isOutcome()));

            if (countOfDetails > 1 && detail.getName() == null) {
                throw new IllegalArgumentException("TransactionDetail name must be provided for multiple details.");
            } else if (countOfDetails == 1 && detail.getName() == null) {
                detail.setName(TransactionDetail.DEFAULT_DETAIL_NAME);
            }

            if (savedTransaction.getTransactionType() == TransactionTypeEnum.INCOME &&
                    detail.getPrice().compareTo(new BigDecimal(0)) != 1) {
                throw new IllegalArgumentException("Income transaction, with a negative or zero detail!");
            }
            if (savedTransaction.getTransactionType() == TransactionTypeEnum.OUTCOME &&
                    detail.getPrice().compareTo(new BigDecimal(0)) != -1) {
                throw new IllegalArgumentException("Expense transaction, with a positive or zero detail!");
            }
            if ((detail.getWeight() != null && detail.getUnitPrice() == null) ||
                    (detail.getWeight() == null && detail.getUnitPrice() != null)) {
                throw new IllegalArgumentException("Weight and unitprice both required, when one of them is set");
            }

            detail.setTransaction(savedTransaction);

            this.transactionDetailRepo.save(detail);
        }
    }

    /**
     * Kiszámolja a tranzakciók alapján, hogy mennyi a jelenlegi pénze a usernek
     * 
     * @return double
     */
    public double sumAllMoney() {
        Double sum = this.transactionRepo.summarizeTotalMoneyForUser(currentUser.getUser().getId());
        return sum == null ? 0 : sum;
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
    public Transaction getTransactionById(Long transactionId) {
        return this.transactionRepo
                .getTransactionById(transactionId, currentUser.getUser().getId())
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
        Transaction transaction = this.getTransactionById(transactionId);
        this.transactionRepo.delete(transaction);
    }
}
