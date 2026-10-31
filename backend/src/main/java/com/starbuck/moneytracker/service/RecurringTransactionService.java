package com.starbuck.moneytracker.service;

import org.springframework.stereotype.Service;

import com.starbuck.moneytracker.repository.RecurringTransactionRepository;

@Service
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepo;

    public RecurringTransactionService(RecurringTransactionRepository recurringTransactionRepo) {
        this.recurringTransactionRepo = recurringTransactionRepo;
    }
}
