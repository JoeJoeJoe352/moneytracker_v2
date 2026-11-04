package com.starbuck.moneytracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bekapcsolja a @Scheduled metódusok futtatását (pl.
 * TransactionIntegrityChecker, RecurringTransactionScheduler).
 * Tesztekben ki van kapcsolva
 */
@Configuration
@EnableScheduling
@Profile("!test")
public class SchedulingConfig {}
