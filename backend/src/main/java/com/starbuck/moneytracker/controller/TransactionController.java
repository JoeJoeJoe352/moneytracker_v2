package com.starbuck.moneytracker.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.dto.MoneySumResponseDto;
import com.starbuck.moneytracker.dto.TransactionCreateRequest;
import com.starbuck.moneytracker.dto.TransactionEditResponseDto;
import com.starbuck.moneytracker.dto.TransactionResponseDto;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.entity.TransactionFilter;
import com.starbuck.moneytracker.mapper.TransactionMapper;
import com.starbuck.moneytracker.service.TransactionService;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;

@RestController
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    public TransactionController(TransactionService transactionService, TransactionMapper transactionMapper) {
        this.transactionService = transactionService;
        this.transactionMapper = transactionMapper;
    }

    @PostMapping(path = "/transaction")
    @ResponseStatus(HttpStatus.CREATED)
    public void createTransaction(@Valid @RequestBody TransactionCreateRequest request) {
        TransactionSaveCommand transaction = transactionMapper.fromTransactionSaveRequest(request);

        this.transactionService.createTransaction(transaction);
    }

    /**
     * Frissíti a usernek a megadott id-jú tranzakcióját
     * 
     * @param TransactionCreateRequest request
     * @param int                      id
     */
    @PutMapping(path = "/transaction/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void updateTransaction(@Valid @RequestBody TransactionCreateRequest request, @PathVariable Long id) {
        TransactionSaveCommand transaction = transactionMapper.fromTransactionSaveRequest(request);

        this.transactionService.updateTransaction(id, transaction);
    }

    /**
     * Összegzi a bevételeket/kiadásokat, visszatér az összes pénzzel
     * 
     * @return double
     */
    @GetMapping(path = "/transaction/sum")
    public MoneySumResponseDto sumAllMoney() {
        return new MoneySumResponseDto(
                this.transactionService.sumAllMoney(),
                this.transactionService.sumAllIncomeForMonth(),
                this.transactionService.sumAllExpenseForMonth().abs());
    }

    /**
     * Visszatér az utolsó x darab tranzakcióval
     * 
     * @return List<TransactionDto>
     */
    @GetMapping(path = "/transaction/last")
    public List<TransactionResponseDto> getLastTransactions() {
        List<Transaction> transactions = this.transactionService.getLastTransactions();
        return this.transactionMapper.toDtoList(transactions);
    }

    /**
     * Visszaadja a user megadott id-jú tranzakcióját
     * 
     * @param id
     * @return TransactionDto
     */
    @GetMapping(path = "/transaction/{id}")
    public TransactionEditResponseDto getTransactionById(@PathVariable Long id) {
        return this.transactionMapper.toEditDto(transactionService.getTransactionByIdForActualUser(id));
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
    public List<TransactionResponseDto> listTransactionHistory(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) LocalDate date) {
        final TransactionFilter filter = new TransactionFilter(name, date);
        List<Transaction> transactions = this.transactionService.getHistoryPageData(filter);
        return this.transactionMapper.toDtoList(transactions);
    }
}
