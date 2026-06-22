package com.starbuck.moneytracker.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.starbuck.moneytracker.dto.TransactionCreateRequest;
import com.starbuck.moneytracker.dto.TransactionDto;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.mapper.TransactionMapper;
import com.starbuck.moneytracker.service.TransactionService;

import io.jsonwebtoken.lang.Arrays;

import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

@RestController
public class TransactionController {
    
    @Autowired
    TransactionService transactionService;
    
    @Autowired
    private TransactionMapper transactionMapper;

    @PostMapping(path = "/transaction/create")
    @ResponseStatus(HttpStatus.CREATED)
    public void createTransaction(@Valid @RequestBody TransactionCreateRequest request, @AuthenticationPrincipal User user) {
        Transaction transaction = new Transaction();
        transaction.setName(request.name());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setTransactionType(request.transactionType());
        transaction.setUser(user);
        
        TransactionDetail TransactionDetailModel = new TransactionDetail();
        TransactionDetailModel.setPrice(request.price());
        try {
            this.transactionService.createTransaction(transaction, TransactionDetailModel);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Transaction create failed: " + e.getMessage());
        }
    }

    /**
     * Összegzi a bevételeket/kiadásokat, visszatér az összes pénzzel
     * 
     * @return
     */
    @GetMapping(path = "transaction/sum")
    public float sumAllMoney() {
        return this.transactionService.sumAllMoney();
    }

    /**
     * Visszatér az utolsó x darab tranzakcióval
     * 
     * @return
     */
    @GetMapping(path = "transaction/last")
    public List<TransactionDto> getLastTransactions() {
        Transaction[] transactions = this.transactionService.getLastTransactions();
        return this.transactionMapper.toDtoList(Arrays.asList(transactions));
    }
}
