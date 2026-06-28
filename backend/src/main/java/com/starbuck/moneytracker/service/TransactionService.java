package com.starbuck.moneytracker.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.util.CurrentUserUtil;
import com.starbuck.moneytracker.util.TransactionSpecifications;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.TransactionFilter;

@Service
public class TransactionService {
    
    /**
     * Ez a neve a transactionDetailnek, hogyha a user összegezve adja meg a tranzakció összeget
     */
    private final String DEFAULT_DETAIL_NAME = "sum";
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
     */
    @Transactional
    public Transaction createTransaction(Transaction transaction, TransactionDetail transactionDetail) {
        try {
            // TODO ha több tranzakció van, akkor össze kell adni őket
            transaction.setPriceSum(transactionDetail.getPrice());
            Transaction transactionModel = this.transactionRepo.save(transaction);
            this.prepareDetail(transactionDetail, transactionModel);
            this.transactionDetailRepo.saveAndFlush(transactionDetail);

            return transactionModel;
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Detail osztályt feltölti a default értékekkel
     */
    private void prepareDetail(TransactionDetail detail, Transaction transaction) {
        detail.setTransaction(transaction);
        // TODO ha csak egy detail van, akkor elfogadható a name null-ság, egyébként hiba
        if (detail.getName() == null) {
            detail.setName(DEFAULT_DETAIL_NAME);
        }
    }

    /**
     * Kiszámolja a tranzakciók alapján, hogy mennyi a jelenlegi pénze a usernek
     * 
     * @return float
     */
    public float sumAllMoney() {
        // TODO kiadások negatív értékek, ezek most nincsenek kezelve
        Float sum = this.transactionRepo.summarizeTotalMoneyForUser(currentUser.getUser().getId());
        return sum == null ? 0 : sum;
    }

    /**
     * Visszatér az utolsó x darab tranzakció objektummal
     * 
     * @return Transaction[]
     */
    public Transaction[] getLastTransactions() {
        return this.transactionRepo.getLastTransactionsForUserWithLimit(currentUser.getUser().getId(), LAST_TRANSACTION_LIMIT);
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
            .getTransactionById(transactionId,  currentUser.getUser().getId())
            .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionId));
    }

    /**
     * Frissíti a user adott id-jú tranzakcióját
     * 
     * @param id
     * @param updatedTransaction
     */
    public void updateTransaction(Long id, Transaction updatedTransaction) {
        // így ellenőrzöm, hogy a tranzakció a useré-e
        Transaction transaction = this.getTransactionById(id);
        
        transaction.setName(updatedTransaction.getName());
        transaction.setPriceSum(updatedTransaction.getPriceSum());
        transaction.setTransactionDate(updatedTransaction.getTransactionDate());
        transaction.setTransactionType(updatedTransaction.getTransactionType());
        this.transactionRepo.save(transaction);
        //TODO detailnak is frissíteni az árát majd
    }

    /**
     * Listázza valamilyen feltételek alapján a tranzakciókat
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
     * Törli a tranzakciót
     * JPA-ban szűrve van, hogy törölt-e és olyankor nem adja vissza (entity-ben vna beállítva)
     * 
     * @param transactionId
     */
    public void deleteTransaction(long transactionId) {
        Transaction transaction = this.getTransactionById(transactionId);
        // 

        this.transactionRepo.delete(transaction);
    }
}
