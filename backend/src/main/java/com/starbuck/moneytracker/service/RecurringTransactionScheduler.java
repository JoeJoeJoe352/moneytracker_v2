package com.starbuck.moneytracker.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.starbuck.moneytracker.entity.RecurringTransaction;

@Component
public class RecurringTransactionScheduler {

    private final RecurringTransactionService recurringTransactionService;
    private final Clock clock;
    private static final Logger logger = LoggerFactory.getLogger(RecurringTransactionScheduler.class);

    public RecurringTransactionScheduler(RecurringTransactionService recurringTransactionService, Clock clock) {
        this.recurringTransactionService = recurringTransactionService;
        this.clock = clock;
    }

    /**
     * Note: ha hosszabb szerverleállás van és kiamrad pár hét, akkor fel fogja
     * venni a régebbi tranzakciókat is, a jó időpontban, de futásonként (jelenleg
     * óránként) csak egyet
     */
    @Scheduled(cron = "0 0 * * * *")
    public void generateRecurrentTransactions() {
        LocalDate actualDate = LocalDate.now(clock);

        List<RecurringTransaction> recurringTransactions =
                this.recurringTransactionService.listRecurringTransactionsForDay(actualDate);

        for (RecurringTransaction recurringTransaction : recurringTransactions) {
            try {
                recurringTransactionService.generateTransactionFromTemplate(recurringTransaction);
            } catch (RuntimeException e) {
                logger.warn("RecurringTransaction with ID {} has error: {}", recurringTransaction.getId(), e);
            }
        }
    }
}
