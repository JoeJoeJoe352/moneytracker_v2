package com.starbuck.moneytracker.config;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

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
     */
    //@Scheduled(cron = "0 */30 * * * *")
    public void checkTransactionIntegrity() {
        logger.info("Checking integrity of the transactions...");
        List<Transaction> transactions = transactionRepository.getAllTransaction();

        /*
        TODO HIBÁT MEGOLDANI
        dev-backend   | org.hibernate.LazyInitializationException: Cannot lazily initialize collection of role 'com.starbuck.moneytracker.entity.TransactionDetail.categoryLinks' with key '74' (no session)
dev-backend   |         at org.hibernate.collection.spi.AbstractPersistentCollection.throwLazyInitializationException(AbstractPersistentCollection.java:647) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
dev-backend   |         at org.hibernate.collection.spi.AbstractPersistentCollection.withTemporarySessionIfNeeded(AbstractPersistentCollection.java:232) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
dev-backend   |         at org.hibernate.collection.spi.AbstractPersistentCollection.initialize(AbstractPersistentCollection.java:620) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
dev-backend   |         at org.hibernate.collection.spi.AbstractPersistentCollection.read(AbstractPersistentCollection.java:142) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
dev-backend   |         at org.hibernate.collection.spi.PersistentBag.iterator(PersistentBag.java:419) ~[hibernate-core-7.2.12.Final.jar:7.2.12.Final]
dev-backend   |         at java.base/java.util.Spliterators$IteratorSpliterator.estimateSize(Spliterators.java:1959) ~[na:na]
dev-backend   |         at java.base/java.util.Spliterator.getExactSizeIfKnown(Spliterator.java:414) ~[na:na]
dev-backend   |         at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:569) ~[na:na]
dev-backend   |         at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:560) ~[na:na]
dev-backend   |         at java.base/java.util.stream.ReduceOps$ReduceOp.evaluateSequential(ReduceOps.java:921) ~[na:na]
dev-backend   |         at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:265) ~[na:na]
dev-backend   |         at java.base/java.util.stream.ReferencePipeline.collect(ReferencePipeline.java:723) ~[na:na]
dev-backend   |         at com.starbuck.moneytracker.entity.TransactionDetail.getCategoryIds(TransactionDetail.java:77) ~[classes/:na]
dev-backend   |         at com.starbuck.moneytracker.mapper.TransactionMapper.lambda$entityToCommand$0(TransactionMapper.java:147) ~[classes/:na]
dev-backend   |         at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:214) ~[na:na]
dev-backend   |         at java.base/java.util.Iterator.forEachRemaining(Iterator.java:133) ~[na:na]
dev-backend   |         at java.base/java.util.Spliterators$IteratorSpliterator.forEachRemaining(Spliterators.java:1939) ~[na:na]
dev-backend   |         at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:570) ~[na:na]
dev-backend   |         at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:560) ~[na:na]
dev-backend   |         at java.base/java.util.stream.ReduceOps$ReduceOp.evaluateSequential(ReduceOps.java:921) ~[na:na]
dev-backend   |         at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:265) ~[na:na]
dev-backend   |         at java.base/java.util.stream.ReferencePipeline.collect(ReferencePipeline.java:723) ~[na:na]
dev-backend   |         at com.starbuck.moneytracker.mapper.TransactionMapper.entityToCommand(TransactionMapper.java:151) ~[classes/:na]
dev-backend   |         at com.starbuck.moneytracker.config.IntegrityConfig.checkTransactionIntegrity(IntegrityConfig.java:43) ~[classes/:na]
dev-backend   |         at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104) ~[na:na]
dev-backend   |         at java.base/java.lang.reflect.Method.invoke(Method.java:565) ~[na:na]
dev-backend   |         at org.springframework.scheduling.support.ScheduledMethodRunnable.runInternal(ScheduledMethodRunnable.java:128) ~[spring-context-7.0.7.jar:7.0.7]
dev-backend   |         at org.springframework.scheduling.support.ScheduledMethodRunnable.lambda$run$1(ScheduledMethodRunnable.java:122) ~[spring-context-7.0.7.jar:7.0.7]
dev-backend   |         at io.micrometer.observation.Observation.observe(Observation.java:569) ~[micrometer-observation-1.16.5.jar:1.16.5]
dev-backend   |         at org.springframework.scheduling.support.ScheduledMethodRunnable.run(ScheduledMethodRunnable.java:122) ~[spring-context-7.0.7.jar:7.0.7]
dev-backend   |         at org.springframework.scheduling.config.Task$OutcomeTrackingRunnable.run(Task.java:88) ~[spring-context-7.0.7.jar:7.0.7]
dev-backend   |         at org.springframework.scheduling.support.DelegatingErrorHandlingRunnable.run(DelegatingErrorHandlingRunnable.java:54) ~[spring-context-7.0.7.jar:7.0.7]
dev-backend   |         at org.springframework.scheduling.concurrent.ReschedulingRunnable.run(ReschedulingRunnable.java:94) ~[spring-context-7.0.7.jar:7.0.7]
dev-backend   |         at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:545) ~[na:na]
dev-backend   |         at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:330) ~[na:na]
dev-backend   |         at java.base/java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:309) ~[na:na]
dev-backend   |         at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1090) ~[na:na]
dev-backend   |         at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:614) ~[na:na]
dev-backend   |         at java.base/java.lang.Thread.run(Thread.java:1516) ~[na:na]
        */
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
