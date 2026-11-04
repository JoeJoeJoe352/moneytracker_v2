package com.starbuck.moneytracker.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
     * Note: ha hosszabb szerverleállás van és kimarad pár hét, akkor fel fogja
     * venni a régebbi tranzakciókat is, a jó időpontban, de futásonként (jelenleg
     * óránként) csak egyet
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void generateRecurrentTransactions() {
        LocalDate actualDate = LocalDate.now(clock);

        List<Long> recurringTransactionIds =
                this.recurringTransactionService.listExpiredRecurringTransactionIds(actualDate);

        // mindegyik külön tranzakcióban fut, így egy hibás nem akasztja meg a többit
        for (Long recurringTransactionId : recurringTransactionIds) {
            try {
                recurringTransactionService.generateTransactionFromTemplate(recurringTransactionId, actualDate);
            } catch (RuntimeException e) {
                logger.error(
                        "Generating transaction failed for RecurringTransaction with ID {}", recurringTransactionId, e);
            }
        }
    }
}
