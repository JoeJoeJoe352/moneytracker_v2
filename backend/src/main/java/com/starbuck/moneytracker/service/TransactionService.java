package com.starbuck.moneytracker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.util.CurrentUserUtil;

import jakarta.transaction.Transactional;

import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.TransactionType;

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
}
