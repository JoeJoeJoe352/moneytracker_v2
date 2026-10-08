package com.starbuck.moneytracker.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.starbuck.moneytracker.entity.BalanceSync;

public interface BalanceSyncRepository extends JpaRepository<BalanceSync, Long> {
    
    Optional<BalanceSync> findBySyncTransactionId(Long transactionId);

    /**
     * Alapból a sync bejegyzések softdelete-el törlődnek, ez a wallet összes
     * (a soft delete-elt) bejegyzését is véglegesen törli
     *
     * @param walletId
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM balance_sync WHERE wallet_id = :walletId", nativeQuery = true)
    void hardDeleteAllForWallet(@Param("walletId") Long walletId);
}
