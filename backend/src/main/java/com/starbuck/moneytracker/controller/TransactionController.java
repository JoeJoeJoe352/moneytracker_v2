package com.starbuck.moneytracker.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Arrays;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.starbuck.moneytracker.dto.TransactionCreateRequest;
import com.starbuck.moneytracker.dto.TransactionDto;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionDetail;
import com.starbuck.moneytracker.entity.TransactionFilter;
import com.starbuck.moneytracker.entity.User;
import com.starbuck.moneytracker.mapper.TransactionMapper;
import com.starbuck.moneytracker.service.TransactionService;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;

@RestController
public class TransactionController {
    
    @Autowired
    TransactionService transactionService;
    
    @Autowired
    private TransactionMapper transactionMapper;

    @PostMapping(path = "/transaction")
    @ResponseStatus(HttpStatus.CREATED)
    public void createTransaction(@Valid @RequestBody TransactionCreateRequest request, @AuthenticationPrincipal @NonNull User user) {
        Transaction transaction = transactionMapper.fromTransactionCreateRequest(request);
        transaction.setUser(user);
        List<TransactionDetail> transactionDetails = transactionMapper.fromDetailCreateRequestList(request.transactionDetails());
        
        try {
            this.transactionService.createTransaction(transaction, transactionDetails);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Transaction create failed: " + e.getMessage());
        }
    }

    /**
     * Frissíti a usernek a megadott id-jú tranzakcióját
     * 
     * @param TransactionCreateRequest request
     * @param int id
     */
    @PutMapping(path = "/transaction/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void updateTransactionSimple(@Valid @RequestBody TransactionCreateRequest request, @PathVariable  Long id) {
        Transaction transaction = new Transaction();
        transaction.setName(request.name());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setTransactionType(request.transactionType());
        transaction.setPriceSum(request.transactionDetails().get(0).price());

        this.transactionService.updateSimpleTransaction(id, transaction);
    }

    /**
     * Összegzi a bevételeket/kiadásokat, visszatér az összes pénzzel
     * 
     * @return double
     */
    @GetMapping(path = "transaction/sum")
    public double sumAllMoney() {
        return this.transactionService.sumAllMoney();
    }

    /**
     * Visszatér az utolsó x darab tranzakcióval
     * 
     * @return List<TransactionDto>
     */
    @GetMapping(path = "transaction/last")
    public List<TransactionDto> getLastTransactions() {
        Transaction[] transactions = this.transactionService.getLastTransactions();
        return this.transactionMapper.toDtoList(Arrays.asList(transactions));
    }

    /**
     * Visszaadja a user megadott id-jú tranzakcióját
     * 
     * @param id
     * @return TransactionDto
     */
    @GetMapping(path = "/transaction/{id}")
    public TransactionDto getTransactionById(@PathVariable Long id) {
        return this.transactionMapper.toDto(transactionService.getTransactionById(id));
    }

    /**
     * Törli a user megadott id-jú tranzakcióját
     * 
     * @param id
     * @return TransactionDto
     */
    @DeleteMapping(path = "/transaction/{id}")
    public void deleteTransactionById(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
    }

    /**
     * Tranzakció history listázása
     * 
     * @param name
     * @param dateString
     * @return
     */
    @GetMapping(path = "/transaction/history")
    public List<TransactionDto> listTransactionHistory(
        @RequestParam(required = false) String name, 
        @RequestParam(required = false) LocalDate date
    ) {
        final TransactionFilter filter = new TransactionFilter(name, date);
        List<Transaction> transactions = this.transactionService.getHistory(filter);
        return this.transactionMapper.toDtoList(transactions);
    }

}
