package com.starbuck.moneytracker.config;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.service.domainservice.CostCalculatorDomainService;

@Configuration
@EnableScheduling
public class IntegrityConfig {

    private final CostCalculatorDomainService costCalculator = new CostCalculatorDomainService();
    private static final Logger logger = LoggerFactory.getLogger(IntegrityConfig.class);
    private TransactionRepository transactionRepository;

    public IntegrityConfig(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Integritás vizsgálatot végez, hogy a tranzakciók összegzett értéke
     * megegyezik-e a cache mezőben tárolt értékkel (price_sum).
     * Ha nem egyezik, akkor logol egy error üzenetet.
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void checkTransactionIntegrity() {
        logger.info("Checking integrity of the transactions...");
        List<Transaction> transactions = transactionRepository.getAllTransaction();

        for (Transaction t : transactions) {
            BigDecimal calculated = costCalculator.calculateTransactionCost(t);
            BigDecimal cached = t.getPriceSum();

            if (cached == null || calculated.compareTo(cached) != 0) {
                logger.warn(
                        "Calculated ({}) and cached ({}) cost of transactionDetails not equals in Transaction with ID {}",
                        calculated,
                        cached,
                        t.getId());
            }
        }
    }
}
