package com.starbuck.moneytracker.service;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.starbuck.moneytracker.commands.TransactionSaveCommand;
import com.starbuck.moneytracker.entity.Transaction;
import com.starbuck.moneytracker.mapper.TransactionMapper;
import com.starbuck.moneytracker.repository.TransactionRepository;
import com.starbuck.moneytracker.service.domainservice.CostCalculatorDomainService;

import jakarta.persistence.EntityManager;

/**
 * Időzített integritás vizsgálat a tranzakciók cache-elt összegére
 */
@Component
public class TransactionIntegrityChecker {

    // Ennyi tranzakciót tölt be egyszerre, hogy ne kerüljön az összes egyszerre memóriába
    static final int BATCH_SIZE = 500;

    private final CostCalculatorDomainService costCalculator = new CostCalculatorDomainService();
    private static final Logger logger = LoggerFactory.getLogger(TransactionIntegrityChecker.class);

    private final TransactionRepository transactionRepository;
    private final TransactionMapper mapper;
    private final EntityManager entityManager;

    public TransactionIntegrityChecker(
            TransactionRepository transactionRepository, TransactionMapper mapper, EntityManager entityManager) {
        this.transactionRepository = transactionRepository;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    /**
     * Integritás vizsgálatot végez, hogy a tranzakciók összegzett értéke
     * megegyezik-e a cache mezőben tárolt értékkel (price_sum).
     * Ha nem egyezik, akkor logol egy warning üzenetet.
     * A tranzakciókat BATCH_SIZE méretű lapokban tölti be.
     *
     * @Transactional szükséges, mert a mapper.entityToCommand() egy lazy
     * kollekciót (TransactionDetail.categoryLinks) olvas - scheduled
     * job-ban nincs web request (nincs Open Session In View), így enélkül
     * LazyInitializationException-t dobna, amint a repository hívás
     * lezárja a sessiont.
     */
    @Transactional(readOnly = true)
    @Scheduled(cron = "0 */30 * * * *")
    public void checkTransactionIntegrity() {
        logger.info("Checking integrity of the transactions...");
        long lastId = 0;

        while (true) {
            List<Long> ids = transactionRepository.getActiveTransactionIdsAfter(lastId, Limit.of(BATCH_SIZE));
            if (ids.isEmpty()) {
                break;
            }

            transactionRepository.getTransactionsWithDetailsByIds(ids).forEach(this::checkTransaction);
            lastId = ids.get(ids.size() - 1);

            // A már ellenőrzött entitásokat kiveszi a persistence contextből, különben
            // a tranzakció végéig ott maradnának, és ugyanúgy elfogyna a memória
            entityManager.clear();
        }
    }

    /**
     * Egy tranzakció ellenőrzése. A hibát elkapja és logolja, hogy egy
     * érvénytelen sor ne állítsa le a teljes futást.
     */
    private void checkTransaction(Transaction transaction) {
        try {
            if (transaction.getTransactionDetails().isEmpty()) {
                // Az alkalmazás minden tranzakcióhoz létrehoz legalább egy detailt, így ez sérült
                // adatot jelez (összeget nem lehet ellenőrizni, mert ilyenkor a mapper a price_sum-ot adja vissza)
                logger.warn("Transaction with ID {} has no transactionDetails", transaction.getId());
                return;
            }

            TransactionSaveCommand command = mapper.entityToCommand(transaction);

            BigDecimal calculated = costCalculator.calculateTransactionCost(command);
            BigDecimal cached = transaction.getPriceSum();

            if (cached == null || calculated.compareTo(cached) != 0) {
                logger.warn(
                        "Calculated ({}) and cached ({}) cost of transactionDetails not equals in Transaction with ID {}",
                        calculated,
                        cached,
                        transaction.getId());
            }
        } catch (RuntimeException e) {
            logger.error("Integrity check failed for Transaction with ID {}", transaction.getId(), e);
        }
    }
}
