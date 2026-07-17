package com.starbuck.moneytracker.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.repository.TransactionRepository;

@Configuration
@EnableScheduling
public class IntegrityConfig {

    Logger logger = LoggerFactory.getLogger(IntegrityConfig.class);
    TransactionRepository transactionRepository;

    public IntegrityConfig(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Integritás vizsgálatot végez, hogy a tranzakciók összegzett értéke megegyezik-e a cache mezőben tárolt értékkel (price_sum).
     * Ha nem egyezik, akkor logol egy warning üzenetet.
     */
    @Scheduled(cron = "*/10 * * * * *")
    public void checkTransactionIntegrity() {
        logger.info("Checking integrity of the transactions...");
        List<Transaction> transactions = transactionRepository.getAllTransaction();

        transactions.stream()
            .filter(t -> t.getPriceSum() == null ||
                    t.getPriceSum() < 0 ||
                    t.sumDetailsCost() != t.getPriceSum())
            .forEach(t -> logger.warn(
                    "Calculated ({}) and cached ({}) cost of transactionDetails not equals in Transaction with ID {}",
                    t.sumDetailsCost(),
                    t.getPriceSum(),
                    t.getId()));
    }
}
