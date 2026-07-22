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
import com.starbuck.moneytracker.util.CurrentUserUtil;
import com.starbuck.moneytracker.util.TransactionSpecifications;

import jakarta.persistence.EntityNotFoundException;

import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.TransactionFilter;

@Service
public class TransactionService {

    /**
     * Ez a neve a transactionDetailnek, hogyha a user összegezve adja meg a
     * tranzakció összeget
     */
    public final String DEFAULT_DETAIL_NAME = "sum";
    /**
     * Utolsó hány tranzakcióval térjünk vissza?
     */
    private final int LAST_TRANSACTION_LIMIT = 5;

    @Autowired
    private TransactionRepository transactionRepo;

    @Autowired
    private TransactionDetailRepository transactionDetailRepo;

    @Autowired
    private CurrentUserUtil currentUser;

    /**
     * Tranzakció létrehozása
     * Ha hiba van, magától rollbackel a spring
     */
    @Transactional
    public Transaction createTransaction(Transaction transaction, List<TransactionDetail> transactionDetails) {
        BigDecimal sumOfDetailsPrice = transactionDetails.stream()
                .map(TransactionDetail::getCost)
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

        BigDecimal sumOfDetailsPrice = updatedDetails.stream()
                .map(TransactionDetail::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        transaction.setPriceSum(sumOfDetailsPrice);
        transaction.setName(updatedTransaction.getName());
        transaction.setTransactionDate(updatedTransaction.getTransactionDate());
        transaction.setTransactionType(updatedTransaction.getTransactionType());

        this.transactionRepo.save(transaction);
        // egyszerűbb törölni a detailokat, mint kikeresni a meglévőket és frissíteni
        this.transactionDetailRepo.deleteAll(transaction.getTransactionDetails());
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
            if (countOfDetails > 1 && detail.getName() == null) {
                throw new IllegalArgumentException("TransactionDetail name must be provided for multiple details.");
            } else if (countOfDetails == 1 && detail.getName() == null) {
                detail.setName(DEFAULT_DETAIL_NAME);
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
        // TODO kiadások negatív értékek, ezek most nincsenek kezelve
        Double sum = this.transactionRepo.summarizeTotalMoneyForUser(currentUser.getUser().getId());
        return sum == null ? 0 : sum;
    }

    /**
     * Visszatér az utolsó x darab tranzakció objektummal
     * 
     * @return Transaction[]
     */
    public Transaction[] getLastTransactions() {
        return this.transactionRepo.getLastTransactionsForUserWithLimit(currentUser.getUser().getId(),
                LAST_TRANSACTION_LIMIT);
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
