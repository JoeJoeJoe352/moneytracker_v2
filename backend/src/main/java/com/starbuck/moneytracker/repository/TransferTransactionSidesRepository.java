package com.starbuck.moneytracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.starbuck.moneytracker.entity.TransferTransactionSide;

public interface TransferTransactionSidesRepository extends JpaRepository<TransferTransactionSide, Long> {}
