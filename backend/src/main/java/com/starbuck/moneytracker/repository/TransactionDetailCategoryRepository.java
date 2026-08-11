package com.starbuck.moneytracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.starbuck.moneytracker.entity.TransactionDetailCategory;

public interface TransactionDetailCategoryRepository extends JpaRepository<TransactionDetailCategory, Long> {

}
