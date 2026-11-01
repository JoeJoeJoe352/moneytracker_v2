package com.starbuck.moneytracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.starbuck.moneytracker.entity.RecurringTransactionCategory;

public interface RecurringTransactionCategoryRepository extends JpaRepository<RecurringTransactionCategory, Long> {}
