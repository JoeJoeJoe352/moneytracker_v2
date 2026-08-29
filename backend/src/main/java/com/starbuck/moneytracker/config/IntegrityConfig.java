package com.starbuck.moneytracker.config;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import com.starbuck.moneytracker.commands.TransactionUpdateCommand;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.mapper.TransactionMapper;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.service.domainservice.CostCalculatorDomainService;

@Configuration
@EnableScheduling
public class IntegrityConfig {

    private final CostCalculatorDomainService costCalculator = new CostCalculatorDomainService();
    private static final Logger logger = LoggerFactory.getLogger(IntegrityConfig.class);

    private final TransactionRepository transactionRepository;
    private final TransactionMapper mapper;

    public IntegrityConfig(TransactionRepository transactionRepository, TransactionMapper mapper) {
        this.transactionRepository = transactionRepository;
        this.mapper = mapper;
    }

    /**
     * Integritás vizsgálatot végez, hogy a tranzakciók összegzett értéke
     * megegyezik-e a cache mezőben tárolt értékkel (price_sum).
     * Ha nem egyezik, akkor logol egy error üzenetet.
     *
     * @Transactional szükséges, mert a mapper.entityToCommand() egy lazy
     * kollekciót (TransactionDetail.categoryLinks) olvas - scheduled
     * job-ban nincs web request (nincs Open Session In View), így enélkül
     * LazyInitializationException-t dobna, amint a repository hívás
     * lezárja a sessiont.
     */
    @Transactional(readOnly = true)
    //@Scheduled(cron = "0 */30 * * * *")
    public void checkTransactionIntegrity() {
        logger.info("Checking integrity of the transactions...");
        List<Transaction> transactions = transactionRepository.getAllTransaction();

        for (Transaction transaction : transactions) {
            TransactionUpdateCommand command = mapper.entityToCommand(transaction);

            BigDecimal calculated = costCalculator.calculateTransactionCost(command);
            BigDecimal cached = transaction.getPriceSum();

            if (cached == null || calculated.compareTo(cached) != 0) {
                logger.warn(
                        "Calculated ({}) and cached ({}) cost of transactionDetails not equals in Transaction with ID {}",
                        calculated,
                        cached,
                        transaction.getId());
            }
        }
    }
}
