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
    @Scheduled(cron = "0 */30 * * * *")
    public void checkTransactionIntegrity() {
        // TODO 
        /**
         * dev-backend   | java.lang.IllegalArgumentException: Global Price must not be set when there is detail
dev-backend   |         at com.starbuck.moneytracker.commands.TransactionSaveCommand.<init>(TransactionSaveCommand.java:39) ~[classes/:na]
dev-backend   |         at com.starbuck.moneytracker.commands.TransactionUpdateCommand.<init>(TransactionUpdateCommand.java:22) ~[classes/:na]
dev-backend   |         at com.starbuck.moneytracker.mapper.TransactionMapper.entityToCommand(TransactionMapper.java:209) ~[classes/:na]
dev-backend   |         at com.starbuck.moneytracker.config.IntegrityConfig.checkTransactionIntegrity(IntegrityConfig.java:52) ~[classes/:na]
dev-backend   |         at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104) ~[na:na]
dev-backend   |         at java.base/java.lang.reflect.Method.invoke(Method.java:565) ~[na:na]
dev-backend   |         at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(AopUtils.java:359) ~[spring-aop-7.0.7.jar:7.0.7]
dev-backend   |         at org.springframework.aop.framework.ReflectiveMethodInvocation.invokeJoinpoint(ReflectiveMethodInvocation.java:190) ~[spring-aop-7.0.7.jar:7.0.7]
dev-backend   |         at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:158) ~[spring-aop-7.0.7.jar:7.0.7]
dev-backend   |         at org.springframework.transaction.interceptor.TransactionInterceptor$1.proceedWithInvocation(TransactionInterceptor.java:133) ~[spring-tx-7.0.7.jar:7.0.7]
dev-backend   |         at org.springframework.transaction.interceptor.TransactionAspectSupport.invokeWithinTransaction(TransactionAspectSupport.java:371) ~[spring-tx-7.0.7.jar:7.0.7]
dev-backend   |         at org.springframework.transaction.interceptor.TransactionInterceptor.invoke(TransactionInterceptor.java:130) ~[spring-tx-7.0.7.jar:7.0.7]
dev-backend   |         at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:179) ~[spring-aop-7.0.7.jar:7.0.7]
dev-backend   |         at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:719) ~[spring-aop-7.0.7.jar:7.0.7]
dev-backend   |         at com.starbuck.moneytracker.config.IntegrityConfig$$SpringCGLIB$$1.checkTransactionIntegrity(<generated>) ~[classes/:na]
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
