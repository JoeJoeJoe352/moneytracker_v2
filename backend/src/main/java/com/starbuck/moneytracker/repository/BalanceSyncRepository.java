package com.starbuck.moneytracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.starbuck.moneytracker.entity.BalanceSync;

public interface BalanceSyncRepository extends JpaRepository<BalanceSync, Long> {

}
