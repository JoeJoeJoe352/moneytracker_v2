package com.starbuck.moneytracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.starbuck.moneytracker.entity.Wallet;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    @Query("SELECT w FROM Wallet w WHERE w.id=?1 AND w.user.id = ?2 AND w.status = 0")
    Optional<Wallet> getWalletById(long walletId, long userId);

    /**
     * A user walletjeinek lekérdezése
     */
    List<Wallet> findByUserId(long userId);
}
