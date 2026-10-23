package com.starbuck.moneytracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bekapcsolja a @Scheduled metódusok futtatását (pl.
 * TransactionIntegrityChecker)
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
