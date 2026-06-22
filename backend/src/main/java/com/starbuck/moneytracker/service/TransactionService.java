package com.starbuck.moneytracker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.repository.TransactionDetailRepository;
import com.starbuck.moneytracker.repository.TransactionRepository;

import jakarta.transaction.Transactional;

import com.starbuck.moneytracker.entity.TransactionDetail;

@Service
public class TransactionService {
    
    /**
     * Ez a neve a transactionDetailnek, hogyha a user összegezve adja meg a tranzakció összeget
     */
    private final String DEFAULT_DETAIL_NAME = "sum";

    @Autowired
    private TransactionRepository transactionRepo;

    @Autowired
    private TransactionDetailRepository transactionDetailRepo;

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
        if (detail.getName() == null) {
            detail.setName(DEFAULT_DETAIL_NAME);
        }
    }
}
