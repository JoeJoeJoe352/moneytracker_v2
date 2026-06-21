package com.starbuck.moneytracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.starbuck.moneytracker.entity.TransactionDetail;

public interface TransactionDetailRepository extends JpaRepository<TransactionDetail, Long> {
    
}
