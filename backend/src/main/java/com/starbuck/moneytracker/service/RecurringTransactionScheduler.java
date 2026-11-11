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
     * 15 percenként létrehozza az esedékes ismétlődő tranzakciókat. A lemaradt
     * előfordulásokat (pl. hónapon belül korábbi kezdés, vagy szerverleállás miatt)
     * is mind létrehozza a saját napjukra, ismétlődő tranzakciónként futásonként
     * legfeljebb RecurringTransactionService.MAX_CATCH_UP_PER_RUN darabot
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void generateRecurrentTransactions() {
        LocalDate actualDate = LocalDate.now(clock);

        List<Long> recurringTransactionIds =
                this.recurringTransactionService.listExpiredRecurringTransactionIds(actualDate);

        // mindegyik külön tranzakcióban fut, így egy hibás nem akasztja meg a többit
        for (Long recurringTransactionId : recurringTransactionIds) {
            try {
                int created =
                        recurringTransactionService.generateTransactionFromTemplate(recurringTransactionId, actualDate);
                if (created > 1) {
                    logger.info(
                            "Generated {} missed transactions for RecurringTransaction with ID {}",
                            created,
                            recurringTransactionId);
                }
            } catch (RuntimeException e) {
                logger.error(
                        "Generating transaction failed for RecurringTransaction with ID {}", recurringTransactionId, e);
            }
        }
    }
}
