package com.starbuck.moneytracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.starbuck.moneytracker.entity.Wallet;

public interface WalletRepository extends JpaRepository<Wallet, Long>  {
    boolean existsByNameAndUserId(String walletName, long userId);
}
