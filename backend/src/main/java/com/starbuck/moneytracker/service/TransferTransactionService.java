package com.starbuck.moneytracker.service;

import org.springframework.stereotype.Service;

import com.starbuck.moneytracker.commands.TransferTransactionCreateCommand;
import com.starbuck.moneytracker.commands.TransferTransactionUpdateCommand;
import com.starbuck.moneytracker.repository.TransferTransactionSidesRepository;

@Service
public class TransferTransactionService {

    private final TransactionService transactionService;
    private final TransferTransactionSidesRepository repository;

    public TransferTransactionService(
            TransactionService transactionService, TransferTransactionSidesRepository repository) {
        this.transactionService = transactionService;
        this.repository = repository;
    }

    public void createLinkedTransaction(TransferTransactionCreateCommand command) {}

    public void updateLinkedTransaction(long transferId, TransferTransactionUpdateCommand command) {}
}
