package com.starbuck.moneytracker.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.starbuck.moneytracker.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long>{
    
}
