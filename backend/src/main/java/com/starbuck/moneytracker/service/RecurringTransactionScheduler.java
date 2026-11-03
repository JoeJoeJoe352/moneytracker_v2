package com.starbuck.moneytracker.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.entity.RecurringTransaction;
import com.starbuck.moneytracker.repository.RecurringTransactionRepository;
import com.starbuck.moneytracker.util.TransactionFactory;

@Component
public class RecurringTransactionScheduler {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final TransactionService transactionService;
    private final RecurringTransactionService recurringTransactionService;
    private final TransactionFactory transactionFactory;
    private final Clock clock;

    public RecurringTransactionScheduler(
            TransactionService transactionService,
            RecurringTransactionService recurringTransactionService,
            Clock clock,
            RecurringTransactionRepository recurringTransactionRepository,
            TransactionFactory transactionFactory) {
        this.transactionService = transactionService;
        this.recurringTransactionService = recurringTransactionService;
        this.clock = clock;
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.transactionFactory = transactionFactory;
    }

    // @Scheduled(cron = "0 */30 * * * *")
    public void generateRecurrentTransactions() {
        LocalDate date = LocalDate.now(clock);

        List<RecurringTransaction> recurringTransactions =
                this.recurringTransactionService.listRecurringTransactionsForDay(date);

        for (RecurringTransaction recurringTransaction : recurringTransactions) {
            TransactionSaveCommand command =
                    transactionFactory.createTransactionCommandFromRecurrentTransactionTemplate(
                            recurringTransaction, LocalDate.now(clock));
            transactionService.createTransaction(command);

            LocalDate nextOccurence =
                    recurringTransaction.getRecurrenceInterval().calculateNextOccurence(date);
            recurringTransaction.setNextOccurrenceDate(nextOccurence);
            recurringTransactionRepository.save(recurringTransaction);
        }
    }
}
