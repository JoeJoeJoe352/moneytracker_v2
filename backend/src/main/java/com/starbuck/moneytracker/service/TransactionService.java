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
    
    @Autowired
    private TransactionRepository transactionRepo;

    @Autowired
    private TransactionDetailRepository transactionDetailRepo;

    @Transactional
    public Transaction createTransaction(Transaction transaction, TransactionDetail transactionDetail) {
        try {
            Transaction transactionModel = this.transactionRepo.save(transaction);

            transactionDetail.setTransaction(transactionModel);
            transactionDetail.setName("sum");
            this.transactionDetailRepo.save(transactionDetail);

            return transactionModel;
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new IllegalArgumentException();
        }
    }

}
