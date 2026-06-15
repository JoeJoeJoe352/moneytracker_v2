package com.starbuck.moneytracker.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import com.starbuck.moneytracker.dto.TransactionCreateRequest;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.service.TransactionService;

import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

@RestController
public class TransactionController {
    
    @Autowired
    TransactionService transactionService;
    
    @PostMapping(path = "/transaction/create")
    @ResponseStatus(HttpStatus.CREATED)
    public void createTransaction(@Valid @RequestBody TransactionCreateRequest request) {
        Transaction transaction = new Transaction();
        transaction.setName(request.name());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setTransactionType(request.transactionType());

        TransactionDetail TransactionDetailModel = new TransactionDetail();
        TransactionDetailModel.setPrice(request.price());
        try {
            this.transactionService.createTransaction(transaction, TransactionDetailModel);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Transaction create failed: " + e.getMessage());
        }
    }
}
